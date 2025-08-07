package com.zelretch.aniiiiiict.data.repository

import co.anilist.GetMediaQuery
import com.zelretch.aniiiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiiict.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepositoryImpl @Inject constructor(
    private val apolloClient: AniListApolloClient,
    private val logger: Logger
) : AniListRepository {

    private val TAG = "AniListRepositoryImpl"

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
                logger.info(
                    TAG,
                    "AniList GraphQLエラー: ${response.errors?.firstOrNull()?.message}",
                    "AniListRepositoryImpl.getMedia"
                )
                return Result.failure(
                    RuntimeException(
                        response.errors?.firstOrNull()?.message ?: "Unknown AniList GraphQL error"
                    )
                )
            }

            val media = response.data?.Media
            if (media == null) {
                logger.info(TAG, "AniList Mediaデータがnullです", "AniListRepositoryImpl.getMedia")
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
                ))
        } catch (e: Exception) {
            logger.error(TAG, e, "AniList Mediaの取得に失敗しました")
            Result.failure(e)
        }
    }
}