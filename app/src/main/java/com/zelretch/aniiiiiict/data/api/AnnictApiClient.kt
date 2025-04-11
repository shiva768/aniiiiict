package com.zelretch.aniiiiiict.data.api

import com.google.gson.GsonBuilder
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.model.rest.AnnictProgram
import com.zelretch.aniiiiiict.util.Logger
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictApiClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, DateTimeTypeAdapter())
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AnnictConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(createOkHttpClient())
        .build()

    private val api = retrofit.create(AnnictApiService::class.java)

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    throw Exception("API error: 401 - No access token")
                }
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .method(original.method, original.body)
                    .build()
                Logger.logInfo(
                    "APIリクエスト: ${request.method} ${request.url}",
                    "AnnictApiClient"
                )
                chain.proceed(request)
            }
            .build()
    }

    suspend fun getWatchingWorks(): Result<List<AnnictWork>> {
        return try {
            if (!tokenManager.hasValidToken()) {
                return Result.failure(Exception("API error: 401 - No valid token"))
            }
            val response = api.getWatchingWorks()
            if (response.isSuccessful) {
                Result.success(response.body()?.works ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWantToWatchWorks(): Result<List<AnnictWork>> {
        return try {
            if (!tokenManager.hasValidToken()) {
                return Result.failure(Exception("API error: 401 - No valid token"))
            }
            val response = api.getWantToWatchWorks()
            if (response.isSuccessful) {
                Result.success(response.body()?.works ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrograms(
        unwatched: Boolean
    ): Result<List<AnnictProgram>> {
        return try {
            Logger.logInfo("プログラム一覧を取得中 - unwatched: $unwatched", "AnnictApiClient")
            if (!tokenManager.hasValidToken()) {
                Logger.logWarning("有効なトークンがありません", "AnnictApiClient")
                return Result.failure(Exception("API error: 401 - No valid token"))
            }
            val response = api.getPrograms(unwatched)
            if (response.isSuccessful) {
                val programs = response.body()?.programs ?: emptyList()
                Logger.logInfo(
                    "プログラム一覧の取得に成功: ${programs.size}件",
                    "AnnictApiClient"
                )
                Result.success(programs)
            } else {
                Logger.logError(
                    Exception("API error: ${response.code()}"),
                    "プログラム一覧の取得に失敗 - status: ${response.code()}"
                )
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Logger.logError(e, "プログラム一覧の取得中に例外が発生")
            Result.failure(e)
        }
    }

    suspend fun updateWorkStatus(workId: Long, status: String): Result<Unit> {
        return try {
            if (!tokenManager.hasValidToken()) {
                return Result.failure(Exception("API error: 401 - No valid token"))
            }
            val response = api.updateStatus(workId, status)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRecord(episodeId: Long): Result<Unit> {
        return try {
            if (!tokenManager.hasValidToken()) {
                return Result.failure(Exception("API error: 401 - No valid token"))
            }
            val response = api.createRecord(episodeId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
