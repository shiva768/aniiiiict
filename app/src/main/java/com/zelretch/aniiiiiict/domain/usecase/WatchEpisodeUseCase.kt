package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import javax.inject.Inject

class WatchEpisodeUseCase
    @Inject
    constructor(
        private val repository: AnnictRepository,
        private val updateViewStateUseCase: UpdateViewStateUseCase,
    ) {
        suspend operator fun invoke(
            episodeId: String,
            workId: String,
            currentStatus: StatusState,
            shouldUpdateStatus: Boolean = true,
        ): Result<Unit> {
            return try {
                // エピソードの視聴を記録
                val recordSuccess = repository.createRecord(episodeId, workId)
                if (!recordSuccess) {
                    return Result.failure(Exception("エピソードの記録に失敗しました"))
                }

                // ステータス更新が必要な場合のみ更新
                if (shouldUpdateStatus && currentStatus == StatusState.WANNA_WATCH) {
                    updateViewStateUseCase(workId, StatusState.WATCHING)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
