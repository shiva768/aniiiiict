package com.zelretch.aniiiiiict.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.network.okHttpClient
import com.zelretch.aniiiiiict.BuildConfig
import com.zelretch.aniiiiiict.util.Logger
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListApolloClient @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "AniListApolloClient"
        private const val SERVER_URL = "https://graphql.anilist.co"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val client by lazy {
        ApolloClient.Builder()
            .serverUrl(SERVER_URL)
            .okHttpClient(okHttpClient)
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

            logger.error(
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

            logger.error(
                TAG,
                "GraphQLミューテーションの実行に失敗: ${operation.name()}",
                context
            )
            throw e
        }
    }
} 