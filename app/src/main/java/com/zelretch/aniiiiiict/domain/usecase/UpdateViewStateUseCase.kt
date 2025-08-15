package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.util.Logger
import javax.inject.Inject

class UpdateViewStateUseCase
    @Inject
    constructor(
        private val repository: AnnictRepository,
        private val logger: Logger,
    ) {
        companion object {
            private const val TAG = "UpdateViewStateUseCase"
        }

        suspend operator fun invoke(
            workId: String,
            status: StatusState,
        ): Result<Unit> =
            try {
                val updateSuccess = repository.updateWorkViewStatus(workId, status)
                if (!updateSuccess) {
                    logger.warning(
                        TAG,
                        "ステータスの更新に失敗しました: workId=$workId",
                        "UpdateViewStateUseCase",
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
