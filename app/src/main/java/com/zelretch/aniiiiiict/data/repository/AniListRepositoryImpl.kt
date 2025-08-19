package com.zelretch.aniiiiiict.data.repository

import co.anilist.GetMediaByMalIdQuery
import co.anilist.GetMediaQuery
import com.apollographql.apollo.api.Optional
import com.zelretch.aniiiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.model.NextAiringEpisode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepositoryImpl @Inject constructor(
    private val apolloClient: AniListApolloClient
) : AniListRepository {
    override suspend fun getMedia(mediaId: Int): Result<AniListMedia> {
        return try {
            val query = GetMediaQuery(
                id = mediaId
            )
            val response = apolloClient.executeQuery(
                operation = query,
                context = "AniListRepositoryImpl.getMedia"
            )

            if (response.hasErrors()) {
                Timber.i("AniList GraphQLエラー: ${response.errors?.firstOrNull()?.message}")
                return Result.failure(
                    RuntimeException(
                        response.errors?.firstOrNull()?.message ?: "Unknown AniList GraphQL error"
                    )
                )
            }

            val media = response.data?.Media
            if (media == null) {
                Timber.i("AniList Mediaデータがnullです")
                return Result.failure(RuntimeException("AniList Media data is null"))
            }

            Result.success(
                AniListMedia(
                    id = media.id,
                    format = media.format?.rawValue,
                    episodes = media.episodes,
                    status = media.status?.rawValue,
                    nextAiringEpisode = media.nextAiringEpisode?.let {
                        NextAiringEpisode(
                            episode = it.episode,
                            airingAt = it.airingAt
                        )
                    }
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "AniList Mediaの取得に失敗しました")
            Result.failure(e)
        }
    }

    override suspend fun getMediaByMalId(malId: Int): Result<AniListMedia> {
        return try {
            val query = GetMediaByMalIdQuery(
                idMal = Optional.Present(malId)
            )
            val response = apolloClient.executeQuery(
                operation = query,
                context = "AniListRepositoryImpl.getMediaByMalId"
            )

            if (response.hasErrors()) {
                Timber.i("AniList GraphQLエラー: ${response.errors?.firstOrNull()?.message}")
                return Result.failure(
                    RuntimeException(
                        response.errors?.firstOrNull()?.message ?: "Unknown AniList GraphQL error"
                    )
                )
            }

            val media = response.data?.Media
            if (media == null) {
                Timber.i("AniList Mediaデータがnullです")
                return Result.failure(RuntimeException("AniList Media data is null"))
            }

            Result.success(
                AniListMedia(
                    id = media.id,
                    format = media.format?.rawValue,
                    episodes = media.episodes,
                    status = media.status?.rawValue,
                    nextAiringEpisode = media.nextAiringEpisode?.let {
                        NextAiringEpisode(
                            episode = it.episode,
                            airingAt = it.airingAt
                        )
                    }
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "AniList Mediaの取得に失敗しました")
            Result.failure(e)
        }
    }
}
