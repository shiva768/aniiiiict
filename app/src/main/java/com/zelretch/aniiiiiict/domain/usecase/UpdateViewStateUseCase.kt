package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import timber.log.Timber
import javax.inject.Inject

class UpdateViewStateUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    suspend operator fun invoke(workId: String, status: StatusState): Result<Unit> = try {
        val updateSuccess = repository.updateWorkViewStatus(workId, status)
        if (!updateSuccess) {
            Timber.w(
                "ステータスの更新に失敗しました: workId=$workId"
            )
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
