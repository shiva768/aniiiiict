package com.zelretch.aniiiiict.data.repository

import co.anilist.GetMediaQuery
import com.zelretch.aniiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiict.data.model.AniListMedia
import com.zelretch.aniiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRepositoryImpl @Inject constructor(
    private val apolloClient: AniListApolloClient
) : AniListRepository {
    override suspend fun getMedia(mediaId: Int): Result<AniListMedia> {
        return runCatching {
            val query = GetMediaQuery(id = mediaId)
            val response = apolloClient.executeQuery(
                operation = query,
                context = "AniListRepositoryImpl.getMedia"
            )

            val media = response.data?.Media ?: return Result.failure(
                IOException(
                    response.errors?.firstOrNull()?.message ?: "Media data is null"
                )
            )

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
        }.onFailure { e ->
            ErrorHandler.handleError(e, "AniListRepositoryImpl", "getMedia")
        }
    }
}
