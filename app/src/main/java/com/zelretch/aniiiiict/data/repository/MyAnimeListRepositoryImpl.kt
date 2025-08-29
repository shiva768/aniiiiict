package com.zelretch.aniiiiict.data.repository

import com.zelretch.aniiiiict.BuildConfig
import com.zelretch.aniiiiict.data.api.MyAnimeListApi
import com.zelretch.aniiiiict.data.model.MyAnimeListMedia
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyAnimeListRepositoryImpl @Inject constructor(
    private val api: MyAnimeListApi
) : MyAnimeListRepository {
    
    companion object {
        private val CLIENT_ID = BuildConfig.MAL_CLIENT_ID
    }
    
    override suspend fun getMedia(mediaId: Int): Result<MyAnimeListMedia> {
        return runCatching {
            val response = api.getAnime(
                animeId = mediaId,
                clientId = CLIENT_ID
            )
            
            if (response.isSuccessful) {
                response.body()?.data ?: throw IOException("Response body is null")
            } else {
                throw IOException("API request failed: ${response.code()} ${response.message()}")
            }
        }.onFailure { e ->
            ErrorHandler.handleError(e, "MyAnimeListRepositoryImpl", "getMedia")
        }
    }
}