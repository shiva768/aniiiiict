package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject

/**
 * ライブラリの「まとめて記録」用に、作品の未視聴エピソードを話数順で返す。
 *
 * Annict の nextEpisode（最後に記録した話の次）以降を未視聴とみなす。「見た」ボタンと同じ基準。
 * nextEpisode が一覧に見つからない場合は viewerDidTrack=false の話にフォールバックする。
 */
class LoadUnwatchedEpisodesUseCase @Inject constructor(
    private val annictRepository: AnnictRepository
) {
    suspend operator fun invoke(workId: String, nextEpisodeId: String?): Result<List<Episode>> =
        annictRepository.getWorkEpisodes(workId).map { episodes ->
            val startIndex = episodes.indexOfFirst { it.id == nextEpisodeId }
            val unwatched = if (startIndex >= 0) {
                episodes.drop(startIndex)
            } else {
                episodes.filter { it.viewerDidTrack != true }
            }
            // 最終話判定で使えるよう、各話に次の話があるかを付けておく
            unwatched.mapIndexed { index, episode ->
                episode.copy(hasNextEpisode = index < unwatched.lastIndex)
            }
        }
}
