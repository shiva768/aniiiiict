package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
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
    private val myAnimeListRepository: MyAnimeListRepository
) {
    suspend operator fun invoke(currentEpisodeNumber: Int, mediaId: Int): JudgeFinaleResult {
        Timber.i(
            "最終話判定を開始: currentEpisode=$currentEpisodeNumber, mediaId=$mediaId"
        )

        val result = myAnimeListRepository.getMedia(mediaId)

        return result.fold(onSuccess = { media ->
            when {
                // media_type != tv の場合、最終話判定ロジックをスキップ
                media.mediaType != null && media.mediaType != "tv" -> {
                    Timber.i("フォーマットがTVではないため判定をスキップ: mediaType=${media.mediaType}")
                    JudgeFinaleResult(FinaleState.UNKNOWN, false)
                }
                // MyAnimeListには nextAiringEpisode 相当の情報がないため、
                // status と num_episodes で判定する
                
                // 1. status == finished_airing → finale_confirmed
                media.status == "finished_airing" -> {
                    Timber.i("ステータスが finished_airing のため FINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
                }
                // 2. num_episodes が数値 かつ currentEp >= num_episodes → finale_confirmed
                media.numEpisodes != null && currentEpisodeNumber >= media.numEpisodes -> {
                    Timber.i("現在のエピソードが総エピソード数に達したためFINALE_CONFIRMED")
                    JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
                }
                // 3. status == currently_airing かつ num_episodes が null → unknown
                media.status == "currently_airing" && media.numEpisodes == null -> {
                    Timber.i("現在放送中で総エピソード数が不明のためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN, false)
                }
                // 4. status == currently_airing → not_finale
                media.status == "currently_airing" -> {
                    Timber.i("現在放送中のためNOT_FINALE")
                    JudgeFinaleResult(FinaleState.NOT_FINALE, false)
                }
                // 5. それ以外 → unknown
                else -> {
                    Timber.i("判定条件に合致しないためUNKNOWN")
                    JudgeFinaleResult(FinaleState.UNKNOWN, false)
                }
            }
        }, onFailure = { e ->
            Timber.e(e, "MyAnimeListからの作品情報取得に失敗しました")
            JudgeFinaleResult(FinaleState.UNKNOWN, false) // エラー時は不明として扱う
        })
    }
}
