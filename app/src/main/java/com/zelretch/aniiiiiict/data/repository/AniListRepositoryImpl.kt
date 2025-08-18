package com.zelretch.aniiiiiict.data.repository

import co.anilist.GetMediaQuery
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
                Timber.i(
                    "AniListRepositoryImpl",
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
                Timber.i("AniListRepositoryImpl", "AniList Mediaデータがnullです", "AniListRepositoryImpl.getMedia")
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
            Timber.e("AniListRepositoryImpl", e, "AniList Mediaの取得に失敗しました")
            Result.failure(e)
        }
    }
}
