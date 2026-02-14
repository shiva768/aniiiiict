package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import timber.log.Timber
import javax.inject.Inject

class WatchEpisodeUseCase @Inject constructor(
    private val repository: AnnictRepository,
    private val updateViewStateUseCase: UpdateViewStateUseCase
) {
    suspend operator fun invoke(
        episodeId: String,
        workId: String,
        currentStatus: StatusState,
        shouldUpdateStatus: Boolean = true
    ): Result<Unit> = runCatching {
        // ステータス更新が必要な場合は先に更新
        if (shouldUpdateStatus && currentStatus == StatusState.WANNA_WATCH) {
            updateViewStateUseCase(workId, StatusState.WATCHING).getOrThrow()
        }

        // episodeId が空のときは記録をスキップ（ViewModel の既存呼び出しに対応）
        if (episodeId.isNotBlank()) {
            repository.createRecord(episodeId, workId).getOrThrow()
        }
    }.onFailure { e ->
        Timber.e(e, "WatchEpisodeUseCase.invoke failed")
    }
}
