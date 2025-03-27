package com.zelretch.aniiiiiict.data.api

import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.model.AnnictResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

object AnnictConfig {
    const val BASE_URL = "https://api.annict.com/v1/"
    const val AUTH_URL = "https://api.annict.com/oauth/authorize"
    const val TOKEN_URL = "https://api.annict.com/oauth/token"
}

interface AnnictApiService {
    @GET("me/works")
    suspend fun getWorks(
        @Query("filter_status") status: String? = null,
        @Query("filter_season") season: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("sort_started_at") sortStartedAt: String = "desc"
    ): Response<AnnictResponse<AnnictWork>>

    @POST("me/statuses")
    suspend fun updateStatus(
        @Query("work_id") workId: Long,
        @Query("kind") kind: String
    ): Response<Unit>

    @GET("me/works")
    suspend fun getWatchingWorks(
        @Query("filter_status") status: String = "watching",
        @Query("sort_started_at") sortStartedAt: String = "asc"
    ): Response<AnnictResponse<AnnictWork>>

    @GET("me/works")
    suspend fun getWantToWatchWorks(
        @Query("filter_status") status: String = "wanna_watch",
        @Query("sort_started_at") sortStartedAt: String = "asc"
    ): Response<AnnictResponse<AnnictWork>>
}