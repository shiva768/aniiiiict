package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.apollographql.apollo.exception.ApolloException
import java.io.IOException
import javax.inject.Inject

class BulkRecordEpisodesUseCase @Inject constructor(
    private val watchEpisodeUseCase: WatchEpisodeUseCase
) {
    suspend operator fun invoke(
        episodeIds: List<String>,
        workId: String,
        currentStatus: StatusState,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> {
        return try {
            if (episodeIds.isEmpty()) {
                return Result.success(Unit)
            }

            // 最初のエピソードで状態を更新
            val firstEpisodeId = episodeIds.first()
            watchEpisodeUseCase(firstEpisodeId, workId, currentStatus, true).getOrThrow()
            onProgress(1)

            // 残りのエピソードは視聴記録のみ
            episodeIds.drop(1).forEachIndexed { index, id ->
                watchEpisodeUseCase(id, workId, currentStatus, false).getOrThrow()
                onProgress(index + 2)
            }

            Result.success(Unit)
        } catch (e: ApolloException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
