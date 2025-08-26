package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AniListRepository
import timber.log.Timber
import javax.inject.Inject

enum class FinaleState {
    NOT_FINALE,
    FINALE_CONFIRMED,
    FINALE_EXPECTED,
    UNKNOWN
}

data class JudgeFinaleResult(val state: FinaleState, val isFinale: Boolean)

class JudgeFinaleUseCase @Inject constructor(
    private val aniListRepository: AniListRepository
) {
    suspend operator fun invoke(currentEpisodeNumber: Int, mediaId: Int): JudgeFinaleResult {
        Timber.i(
            "最終話判定を開始: currentEpisode=$currentEpisodeNumber, mediaId=$mediaId"
        )

        val result = aniListRepository.getMedia(mediaId)

        return result.fold(onSuccess = { media ->
            when {
                // format != TV の場合、最終話判定ロジックをスキップ
                media.format != null && media.format != "TV" -> {
                    Timber.i("フォーマットがTVではないため判定をスキップ: format=${media.format}")
                    JudgeFinaleResult(FinaleState.UNKNOWN, false)
                }
                // 1. 次回予定ありかつ nextAiringEpisode.episode > currentEp → not_finale
                media.nextAiringEpisode?.episode?.let { it > currentEpisodeNumber } == true -> {
                    Timber.i("次回エピソードが現在のエピソードより大きいためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE, false)
                }
                // 2. episodes が数値 かつ currentEp >= episodes かつ nextAiringEpisode == null → finale_confirmed
                media.episodes != null && currentEpisodeNumber >= media.episodes && media.nextAiringEpisode == null -> {
                    Timber.i("総エピソード数と一致し、次回エピソードがないためFINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
                }
                // 3. status == FINISHED かつ nextAiringEpisode == null → finale_confirmed
                media.status == "FINISHED" && media.nextAiringEpisode == null -> {
                    Timber.i("ステータスがFINISHEDで、次回エピソードがないためFINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
                }
                // 4. nextAiringEpisode == null（ただし 2,3 未満足） → finale_expected
                media.nextAiringEpisode == null -> {
                    Timber.i("次回エピソードがないためFINALE_EXPECTED")
                    JudgeFinaleResult(FinaleState.FINALE_EXPECTED, false)
                }
                // 5. それ以外 → unknown
                else -> {
                    Timber.i("判定条件に合致しないためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN, false)
                }
            }
        }, onFailure = { e ->
            Timber.e(e, "AniListからの作品情報取得に失敗しました")
            JudgeFinaleResult(FinaleState.UNKNOWN, false) // エラー時は不明として扱う
        })
    }
}
