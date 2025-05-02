package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.type.StatusState
import kotlinx.coroutines.delay
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
            // チャンクサイズを5に設定（APIのレート制限を考慮）
            val chunkSize = 5
            val chunks = episodeIds.chunked(chunkSize)

            chunks.forEachIndexed { index, chunk ->
                chunk.forEach { id ->
                    watchEpisodeUseCase(id, workId, currentStatus).getOrThrow()
                    onProgress(index * chunkSize + chunk.indexOf(id) + 1)
                }
                // チャンク間で少し待機（APIのレート制限を考慮）
                delay(100)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 