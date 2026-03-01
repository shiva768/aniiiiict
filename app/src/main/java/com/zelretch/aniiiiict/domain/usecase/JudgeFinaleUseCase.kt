package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import timber.log.Timber
import javax.inject.Inject

enum class FinaleState {
    NOT_FINALE,
    FINALE_CONFIRMED,
    UNKNOWN
}

data class JudgeFinaleResult(val state: FinaleState) {
    val isFinale: Boolean
        get() = state == FinaleState.FINALE_CONFIRMED
}

class JudgeFinaleUseCase @Inject constructor(
    private val myAnimeListRepository: MyAnimeListRepository
) {
    suspend operator fun invoke(
        currentEpisodeNumber: Int,
        animeId: Int,
        hasNextEpisode: Boolean? = null
    ): JudgeFinaleResult {
        Timber.i(
            "最終話判定を開始: currentEpisode=$currentEpisodeNumber, animeId=$animeId, hasNextEpisode=$hasNextEpisode"
        )

        val result = myAnimeListRepository.getAnimeDetail(animeId)

        return result.fold(onSuccess = { media ->
            when {
                // 1. Annictにnextエピソードがあれば最終話ではない（最優先）
                // 1期・2期問題も含め、次話の存在が確認できれば最終話ではない
                hasNextEpisode == true -> {
                    Timber.i("nextEpisodeがtrueのためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE)
                }
                // 2. MALのステータスが放送中なら最終話ではない
                // num_episodesが不明でも放送中であれば続きがある
                media.status == "currently_airing" -> {
                    Timber.i("現在放送中のためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE)
                }
                // 3. num_episodes に達していたら最終話確定
                media.numEpisodes != null && currentEpisodeNumber >= media.numEpisodes -> {
                    Timber.i("現在のエピソードが総エピソード数に達したためFINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)
                }
                // 4. num_episodes が null で判定できない → unknown
                media.numEpisodes == null -> {
                    Timber.i("総エピソード数が不明で次話情報もないためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN)
                }
                // 5. それ以外 → unknown
                else -> {
                    Timber.i("判定条件に合致しないためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN)
                }
            }
        }, onFailure = { e ->
            Timber.e(e, "MyAnimeListからの作品情報取得に失敗しました")
            JudgeFinaleResult(FinaleState.UNKNOWN) // エラー時は不明として扱う
        })
    }
}
