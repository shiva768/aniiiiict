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
                // MyAnimeListには nextAiringEpisode 相当の情報がないため、
                // status と num_episodes で判定する

                // 1. num_episodes が数値 かつ currentEp >= num_episodes かつ nextEpisode != true → finale_confirmed
                // hasNextEpisode == true の場合は、次話があるので最終話ではない（1期・2期問題を回避）
                media.numEpisodes != null && currentEpisodeNumber >= media.numEpisodes && hasNextEpisode != true -> {
                    Timber.i("現在のエピソードが総エピソード数に達し、nextEpisodeがtrueでないためFINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)
                }
                // 2. num_episodes が null でも hasNextEpisode == true なら最終話ではない
                // Annictから次のエピソードがあることが確認できれば、エピソード数不明でも最終話ではない
                media.numEpisodes == null && hasNextEpisode == true -> {
                    Timber.i("総エピソード数は不明だが、nextEpisodeがtrueのためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE)
                }
                // 3. num_episodes が null で hasNextEpisode != true → unknown
                // nextEpisode がない理由は、最終話、情報未登録、放送休止など複数考えられるため判定不能
                media.numEpisodes == null -> {
                    Timber.i("総エピソード数が不明で次話情報もないためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN)
                }
                // 4. status == currently_airing → not_finale
                media.status == "currently_airing" -> {
                    Timber.i("現在放送中のためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE)
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
