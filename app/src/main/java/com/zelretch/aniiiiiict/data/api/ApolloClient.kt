package com.zelretch.aniiiiiict.data.api

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApolloClient @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "ApolloClient"
        private const val SERVER_URL = "https://api.annict.com/graphql"
    }

    private val client by lazy {
        val token = tokenManager.getAccessToken()
        AniiiiiictLogger.logInfo("Apollo初期化 - アクセストークンの有無: ${!token.isNullOrBlank()}", "ApolloClient.init")

        val authenticatedClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "Bearer ${token ?: BuildConfig.ANNICT_ACCESS_TOKEN}"
                    )
                    .build()
                chain.proceed(request)
            }
            .build()

        ApolloClient.Builder()
            .serverUrl(SERVER_URL)
            .okHttpClient(authenticatedClient)
            .build()
    }

    fun getApolloClient(): ApolloClient = client
} 