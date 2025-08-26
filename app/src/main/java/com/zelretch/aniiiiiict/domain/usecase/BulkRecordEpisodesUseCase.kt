package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
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
        return runCatching {
            if (episodeIds.isEmpty()) return@runCatching

            // 最初のエピソードで状態を更新
            val firstEpisodeId = episodeIds.first()
            watchEpisodeUseCase(firstEpisodeId, workId, currentStatus, true).getOrThrow()
            onProgress(1)

            // 残りのエピソードは視聴記録のみ
            episodeIds.drop(1).forEachIndexed { index, id ->
                watchEpisodeUseCase(id, workId, currentStatus, false).getOrThrow()
                onProgress(index + 2)
            }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                // Bulk のエラーハンドリングでは元例外をそのまま返してテスト期待に合わせる
                Result.failure(e)
            }
        )
    }
}
