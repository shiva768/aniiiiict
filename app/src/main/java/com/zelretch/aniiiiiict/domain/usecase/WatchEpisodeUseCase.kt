package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.type.StatusState
import javax.inject.Inject

class WatchEpisodeUseCase @Inject constructor(
    private val repository: AnnictRepository,
    private val updateViewStateUseCase: UpdateViewStateUseCase,
) {
    suspend operator fun invoke(
        episodeId: String,
        workId: String,
        currentStatus: StatusState,
        firstChild: Boolean = true,
    ): Result<Unit> {
        return try {
            // エピソードの視聴を記録
            val recordSuccess = repository.createRecord(episodeId, workId)
            if (!recordSuccess) {
                return Result.failure(Exception("エピソードの記録に失敗しました"))
            }

            // WANNA_WATCH状態の作品を視聴した場合はWATCHINGに更新
            if (currentStatus == StatusState.WANNA_WATCH && firstChild)
                updateViewStateUseCase(workId, StatusState.WATCHING)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 