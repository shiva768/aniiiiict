package com.zelretch.aniiiiiict.data.api

import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.AnnictWork
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class AnnictApiClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(AnnictConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
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
            Log.d("AnnictApiClient", "API Response: ${response.body()}")
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
            Log.d("AnnictApiClient", "API Response: ${response.body()}")
            if (response.isSuccessful) {
                Result.success(response.body()?.works ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
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
}
