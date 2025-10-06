package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.BuildConfig
import com.zelretch.aniiiiict.data.api.MyAnimeListApi
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyAnimeListRepositoryImpl @Inject constructor(
    private val api: MyAnimeListApi
) : MyAnimeListRepository {

    companion object {
        private const val CLIENT_ID = BuildConfig.MAL_CLIENT_ID
    }

    override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> = runCatching {
        val response = api.getAnime(
            animeId = animeId,
            clientId = CLIENT_ID
        )

        if (response.isSuccessful) {
            response.body() ?: throw IOException("Response body is null")
        } else {
            throw IOException("API request failed: ${response.code()} ${response.message()}")
        }
    }.onFailure { e ->
        Timber.e(e, "MyAnimeListRepositoryImpl.getAnimeDetail failed")
    }
}
