package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.repository.AniListRepository
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
            "JudgeFinaleUseCase",
            "最終話判定を開始: currentEpisode=$currentEpisodeNumber, mediaId=$mediaId",
            "JudgeFinaleUseCase.invoke"
        )

        val result = aniListRepository.getMedia(mediaId)

        return result.fold(onSuccess = { media ->
            // format != TV の場合、最終話判定ロジックをスキップ
            if (media.format != null && media.format != "TV") {
                Timber.i(
                    "JudgeFinaleUseCase",
                    "フォーマットがTVではないため判定をスキップ: format=${media.format}",
                    "JudgeFinaleUseCase.invoke"
                )
                return JudgeFinaleResult(FinaleState.UNKNOWN, false)
            }

            // 1. 次回予定ありかつ nextAiringEpisode.episode > currentEp → not_finale
            media.nextAiringEpisode?.let { nextAiring ->
                if (nextAiring.episode > currentEpisodeNumber) {
                    Timber.i(
                        "JudgeFinaleUseCase",
                        "次回エピソードが現在のエピソードより大きいためNOT_FINALE",
                        "JudgeFinaleUseCase.invoke"
                    )
                    return JudgeFinaleResult(FinaleState.NOT_FINALE, false)
                }
            }

            // 2. episodes が数値 かつ currentEp >= episodes かつ nextAiringEpisode == null → finale_confirmed
            if (media.episodes != null && currentEpisodeNumber >= media.episodes &&
                media.nextAiringEpisode == null
            ) {
                Timber.i(
                    "JudgeFinaleUseCase",
                    "総エピソード数と一致し、次回エピソードがないためFINALE_CONFIRMED",
                    "JudgeFinaleUseCase.invoke"
                )
                return JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
            }

            // 3. status == FINISHED かつ nextAiringEpisode == null → finale_confirmed
            if (media.status == "FINISHED" && media.nextAiringEpisode == null) {
                Timber.i(
                    "JudgeFinaleUseCase",
                    "ステータスがFINISHEDで、次回エピソードがないためFINALE_CONFIRMED",
                    "JudgeFinaleUseCase.invoke"
                )
                return JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
            }

            // 4. nextAiringEpisode == null（ただし 2,3 未満足） → finale_expected
            if (media.nextAiringEpisode == null) {
                Timber.i("JudgeFinaleUseCase", "次回エピソードがないためFINALE_EXPECTED", "JudgeFinaleUseCase.invoke")
                return JudgeFinaleResult(FinaleState.FINALE_EXPECTED, false)
            }

            // 5. それ以外 → unknown
            Timber.i("JudgeFinaleUseCase", "判定条件に合致しないためUNKNOWN", "JudgeFinaleUseCase.invoke")
            JudgeFinaleResult(FinaleState.UNKNOWN, false)
        }, onFailure = { e ->
            Timber.e("JudgeFinaleUseCase", e, "AniListからの作品情報取得に失敗しました")
            JudgeFinaleResult(FinaleState.UNKNOWN, false) // エラー時は不明として扱う
        })
    }
}
