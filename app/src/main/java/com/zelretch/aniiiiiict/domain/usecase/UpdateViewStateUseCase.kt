package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.ui.base.ErrorHandler
import timber.log.Timber
import javax.inject.Inject

class UpdateViewStateUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    suspend operator fun invoke(workId: String, status: StatusState): Result<Unit> = runCatching {
        val updateSuccess = repository.updateWorkViewStatus(workId, status)
        if (!updateSuccess) {
            Timber.w("ステータスの更新に失敗しました: workId=$workId")
        }
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { e ->
            val msg = ErrorHandler.handleError(e, "UpdateViewStateUseCase", "invoke")
            Result.failure(Exception(msg))
        }
    )
}
