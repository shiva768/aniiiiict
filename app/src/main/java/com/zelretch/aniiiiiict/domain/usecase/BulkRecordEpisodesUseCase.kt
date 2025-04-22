package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.type.StatusState
import javax.inject.Inject

class BulkRecordEpisodesUseCase @Inject constructor(
    private val watchEpisodeUseCase: WatchEpisodeUseCase
) {
    suspend operator fun invoke(episodeIds: List<String>, workId: String, currentStatus: StatusState): Result<Unit> {
        return try {
            episodeIds.forEach { id ->
                watchEpisodeUseCase(id, workId, currentStatus).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 