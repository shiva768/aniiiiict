package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import javax.inject.Inject

data class BulkRecordResult(
    val finaleResult: JudgeFinaleResult? = null
)

/**
 * フィナーレ判定に必要な情報
 */
data class FinaleJudgmentInfo(
    val malAnimeId: Int?,
    val lastEpisodeNumber: Int?,
    val lastEpisodeHasNext: Boolean?
)

class BulkRecordEpisodesUseCase @Inject constructor(
    private val watchEpisodeUseCase: WatchEpisodeUseCase,
    private val judgeFinaleUseCase: JudgeFinaleUseCase
) {
    suspend operator fun invoke(
        episodeIds: List<String>,
        workId: String,
        currentStatus: StatusState,
        finaleInfo: FinaleJudgmentInfo? = null,
        onProgress: (Int) -> Unit = {}
    ): Result<BulkRecordResult> {
        return runCatching {
            if (episodeIds.isEmpty()) return@runCatching BulkRecordResult()

            // 最初のエピソードで状態を更新
            val firstEpisodeId = episodeIds.first()
            watchEpisodeUseCase(firstEpisodeId, workId, currentStatus, true).getOrThrow()
            onProgress(1)

            // 残りのエピソードは視聴記録のみ
            episodeIds.drop(1).forEachIndexed { index, id ->
                watchEpisodeUseCase(id, workId, currentStatus, false).getOrThrow()
                onProgress(index + 2)
            }

            // 最後のエピソードでフィナーレ判定を実行
            val finaleResult = if (finaleInfo?.malAnimeId != null && finaleInfo.lastEpisodeNumber != null) {
                judgeFinaleUseCase(finaleInfo.lastEpisodeNumber, finaleInfo.malAnimeId, finaleInfo.lastEpisodeHasNext)
            } else {
                null
            }

            BulkRecordResult(finaleResult = finaleResult)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { e ->
                // Bulk のエラーハンドリングでは元例外をそのまま返してテスト期待に合わせる
                Result.failure(e)
            }
        )
    }
}
