package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import javax.inject.Inject

class WatchEpisodeUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    private val TAG = "WatchEpisodeUseCase"
    suspend operator fun invoke(
        episodeId: String,
        workId: String,
        currentStatus: StatusState
    ): Result<Unit> {
        return try {
            // エピソードの視聴を記録
            val recordSuccess = repository.createRecord(episodeId, workId)
            if (!recordSuccess) {
                return Result.failure(Exception("エピソードの記録に失敗しました"))
            }

            // WANNA_WATCH状態の作品を視聴した場合はWATCHINGに更新
            if (currentStatus == StatusState.WANNA_WATCH) {
                val updateSuccess = repository.updateWorkStatus(workId, StatusState.WATCHING)
                if (!updateSuccess) {
                    AniiiiiictLogger.logWarning(
                        TAG,
                        "ステータスの更新に失敗しました: workId=$workId",
                        "WatchEpisodeUseCase"
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 