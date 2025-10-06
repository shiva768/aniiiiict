package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
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
            Timber.e(e, "UpdateViewStateUseCase.invoke failed")
            Result.failure(e)
        }
    )
}
