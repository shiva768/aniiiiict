package com.zelretch.aniiiiiict.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.okHttpClient
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListApolloClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val SERVER_URL = "https://graphql.anilist.co"
    }

    private val client by lazy {
        ApolloClient.Builder().serverUrl(SERVER_URL).okHttpClient(okHttpClient).build()
    }

    suspend fun <D : Query.Data> executeQuery(
        operation: Query<D>,
        context: String,
        cachePolicy: FetchPolicy = FetchPolicy.NetworkFirst
    ): ApolloResponse<D> {
        try {
            return client.query(operation).fetchPolicy(cachePolicy).execute()
        } catch (e: ApolloException) {
            Timber.e(e, "[AniListApolloClient][%s] GraphQLクエリの実行に失敗: %s", context, operation.name())
            throw e
        } catch (e: IOException) {
            Timber.e(e, "[AniListApolloClient][%s] GraphQLクエリの実行に失敗: %s", context, operation.name())
            throw e
        }
    }
}
