package com.zelretch.aniiiiict.data.api

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface MyAnimeListApi {
    @GET("v2/anime/{anime_id}")
    suspend fun getAnime(
        @Path("anime_id") animeId: Int,
        @Header("X-MAL-CLIENT-ID") clientId: String,
        @Query("fields") fields: String = "media_type,num_episodes,status,broadcast"
    ): Response<MyAnimeListResponse>
}
