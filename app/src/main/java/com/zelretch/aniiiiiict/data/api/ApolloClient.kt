package com.zelretch.aniiiiiict.data.api

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.auth.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApolloClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    val client by lazy {
        val token = tokenManager.getAccessToken()
        println("Apollo初期化 - アクセストークン: ${token?.take(10)}...")

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "Bearer ${token ?: BuildConfig.ANNICT_ACCESS_TOKEN}"
                    )
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()

        ApolloClient.Builder()
            .serverUrl("https://api.annict.com/graphql")
            .okHttpClient(okHttpClient)
            .build()
    }

    fun getApolloClient(): ApolloClient {
        return client
    }
} 