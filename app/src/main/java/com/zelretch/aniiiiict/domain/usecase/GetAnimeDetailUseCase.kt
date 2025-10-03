package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

class GetAnimeDetailUseCase @Inject constructor(
    private val annictRepository: AnnictRepository,
    private val myAnimeListRepository: MyAnimeListRepository
) {
    suspend operator fun invoke(programWithWork: ProgramWithWork): Result<AnimeDetailInfo> = runCatching {
        val work = programWithWork.work

        coroutineScope {
            // Annict詳細情報を取得（必須）
            val annictDetailResult = annictRepository.getWorkDetail(work.id)
            if (annictDetailResult.isFailure) {
                throw annictDetailResult.exceptionOrNull()
                    ?: Exception("Annict詳細情報の取得に失敗しました")
            }
            val annictDetail = annictDetailResult.getOrNull()

            // MyAnimeList情報を並行取得（オプショナル）
            val malInfo = work.malAnimeId?.toIntOrNull()?.let { malId ->
                myAnimeListRepository.getAnimeDetail(malId).getOrElse { e ->
                    Timber.w(e, "MyAnimeList情報の取得に失敗しました（続行）")
                    null
                }
            }

            // 統合情報の構築
            val episodeCount = malInfo?.numEpisodes ?: annictDetail?.onWork?.episodesCount

            // 画像のフォールバック: Annict詳細 → MAL → Work既存画像
            val imageUrl = annictDetail?.onWork?.image?.recommendedImageUrl
                ?: malInfo?.mainPicture?.large
                ?: malInfo?.mainPicture?.medium
                ?: work.image?.recommendedImageUrl

            val officialSiteUrl = annictDetail?.onWork?.officialSiteUrl
            val wikipediaUrl = annictDetail?.onWork?.wikipediaUrl

            // Annict詳細情報の取得
            val programs = annictDetail?.onWork?.programs?.nodes
            val seriesList = annictDetail?.onWork?.seriesList?.nodes

            AnimeDetailInfo(
                work = work,
                programs = programs,
                seriesList = seriesList,
                malInfo = malInfo,
                episodeCount = episodeCount,
                imageUrl = imageUrl,
                officialSiteUrl = officialSiteUrl,
                wikipediaUrl = wikipediaUrl
            )
        }
    }.onFailure { e ->
        Timber.e(e, "GetAnimeDetailUseCase.invoke failed")
    }
}
