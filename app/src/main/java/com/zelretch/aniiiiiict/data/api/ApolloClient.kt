package com.zelretch.aniiiiiict.data.api

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.network.okHttpClient
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.util.Logger
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApolloClient @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ApolloClient"
        private const val SERVER_URL = "https://api.annict.com/graphql"
    }

    private val client by lazy {
        val token = tokenManager.getAccessToken()
        logger.logInfo(
            TAG,
            "Apollo初期化 - アクセストークンの有無: ${!token.isNullOrBlank()}",
            "ApolloClient.init"
        )

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

    suspend fun <D : Query.Data> executeQuery(
        operation: Query<D>,
        context: String,
        cachePolicy: FetchPolicy = FetchPolicy.NetworkFirst
    ): ApolloResponse<D> {
        try {
            return client.query(operation)
                .fetchPolicy(cachePolicy)
                .execute()
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            logger.logError(
                TAG,
                "GraphQLクエリの実行に失敗: ${operation.name()}",
                context
            )
            throw e
        }
    }

    suspend fun <D : Mutation.Data> executeMutation(
        operation: Mutation<D>,
        context: String,
        cachePolicy: FetchPolicy = FetchPolicy.NetworkOnly
    ): ApolloResponse<D> {
        try {
            return client.mutation(operation)
                .fetchPolicy(cachePolicy)
                .execute()
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            logger.logError(
                TAG,
                "GraphQLミューテーションの実行に失敗: ${operation.name()}",
                context
            )
            throw e
        }
    }
} 