package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkImage
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.error.DomainError
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
            val annictDetailResult = annictRepository.getWorkDetail(work.id)
            if (annictDetailResult.isFailure) {
                throw annictDetailResult.exceptionOrNull()
                    ?: Exception("Annict詳細情報の取得に失敗しました")
            }
            val annictDetail = annictDetailResult.getOrNull()

            val malInfo = work.malAnimeId?.toIntOrNull()?.let { malId ->
                myAnimeListRepository.getAnimeDetail(malId).getOrElse { e ->
                    Timber.w(e, "MyAnimeList情報の取得に失敗しました（続行）")
                    null
                }
            }

            val episodeCount = (malInfo?.numEpisodes?.takeIf { it > 0 })
                ?: annictDetail?.onWork?.episodesCount?.takeIf { it > 0 }

            val imageUrl = annictDetail?.onWork?.image?.recommendedImageUrl
                ?: malInfo?.mainPicture?.large
                ?: malInfo?.mainPicture?.medium
                ?: work.image?.recommendedImageUrl

            val seriesListResult = annictRepository.getWorkSeriesList(work.id)
            val seriesList = seriesListResult.getOrElse { e ->
                Timber.w(e, "シリーズ情報の取得に失敗しました（続行）")
                null
            }?.onWork?.seriesList?.nodes

            AnimeDetailInfo(
                work = work,
                programs = annictDetail?.onWork?.programs?.nodes,
                casts = annictDetail?.onWork?.casts?.nodes,
                seriesList = seriesList,
                malInfo = malInfo,
                episodeCount = episodeCount,
                imageUrl = imageUrl,
                officialSiteUrl = annictDetail?.onWork?.officialSiteUrl,
                wikipediaUrl = annictDetail?.onWork?.wikipediaUrl
            )
        }
    }.onFailure { e ->
        Timber.e(e, "GetAnimeDetailUseCase.invoke failed")
    }

    suspend operator fun invoke(workId: String): Result<AnimeDetailInfo> = runCatching {
        coroutineScope {
            val annictDetailResult = annictRepository.getWorkDetail(workId)
            if (annictDetailResult.isFailure) {
                throw annictDetailResult.exceptionOrNull()
                    ?: Exception("Annict詳細情報の取得に失敗しました")
            }
            val onWork = annictDetailResult.getOrNull()?.onWork
                ?: throw DomainError.Unknown("作品情報が取得できませんでした")

            val work = Work(
                id = onWork.id,
                title = onWork.title,
                seasonName = onWork.seasonName,
                seasonYear = onWork.seasonYear,
                media = onWork.media.rawValue,
                malAnimeId = onWork.malAnimeId,
                viewerStatusState = onWork.viewerStatusState ?: StatusState.NO_STATE,
                image = onWork.image?.let { img ->
                    WorkImage(
                        recommendedImageUrl = img.recommendedImageUrl,
                        facebookOgImageUrl = img.facebookOgImageUrl
                    )
                }
            )

            val malInfo = onWork.malAnimeId?.toIntOrNull()?.let { malId ->
                myAnimeListRepository.getAnimeDetail(malId).getOrElse { e ->
                    Timber.w(e, "MyAnimeList情報の取得に失敗しました（続行）")
                    null
                }
            }

            val episodeCount = (malInfo?.numEpisodes?.takeIf { it > 0 })
                ?: onWork.episodesCount?.takeIf { it > 0 }

            val imageUrl = onWork.image?.recommendedImageUrl
                ?: malInfo?.mainPicture?.large
                ?: malInfo?.mainPicture?.medium

            val seriesList = annictRepository.getWorkSeriesList(workId).getOrElse { e ->
                Timber.w(e, "シリーズ情報の取得に失敗しました（続行）")
                null
            }?.onWork?.seriesList?.nodes

            AnimeDetailInfo(
                work = work,
                programs = onWork.programs?.nodes,
                casts = onWork.casts?.nodes,
                seriesList = seriesList,
                malInfo = malInfo,
                episodeCount = episodeCount,
                imageUrl = imageUrl,
                officialSiteUrl = onWork.officialSiteUrl,
                wikipediaUrl = onWork.wikipediaUrl
            )
        }
    }.onFailure { e ->
        Timber.e(e, "GetAnimeDetailUseCase.invoke(workId) failed")
    }
}
