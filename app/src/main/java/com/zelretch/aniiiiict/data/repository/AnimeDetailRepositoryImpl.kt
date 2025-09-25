package com.zelretch.aniiiiict.data.repository

import com.annict.WorkDetailQuery
import com.zelretch.aniiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiict.data.model.*
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeDetailRepositoryImpl @Inject constructor(
    private val annictApolloClient: AnnictApolloClient,
    private val myAnimeListRepository: MyAnimeListRepository
) : AnimeDetailRepository {

    override suspend fun getAnimeDetailInfo(workId: String, malAnimeId: String?): Result<AnimeDetailInfo> = runCatching {
        Timber.d("AnimeDetailRepository: getAnimeDetailInfo - workId=$workId, malAnimeId=$malAnimeId")

        val workDetailResponse = annictApolloClient.executeQuery(
            WorkDetailQuery(workId), 
            "getAnimeDetailInfo"
        )
        
        if (workDetailResponse.hasErrors()) {
            val errors = workDetailResponse.errors?.joinToString(", ") { it.message }
            Timber.e("AnimeDetailRepository: GraphQL errors - $errors")
            throw Exception("Failed to fetch work details: $errors")
        }

        val workData = workDetailResponse.data?.node?.onWork
            ?: throw Exception("Work not found or invalid response")

        // Get MyAnimeList data if available
        var malData: MyAnimeListResponse? = null
        if (!malAnimeId.isNullOrEmpty()) {
            val malId = malAnimeId.toIntOrNull()
            if (malId != null) {
                myAnimeListRepository.getAnimeDetail(malId).getOrNull()?.let { response ->
                    malData = response
                    Timber.d("AnimeDetailRepository: Got MAL data - episodes=${response.numEpisodes}")
                }
            }
        }

        // Convert streaming platforms
        val streamingPlatforms = workData.programs?.nodes?.mapNotNull { program ->
            program?.let {
                StreamingPlatform(
                    id = it.id,
                    name = it.channel.name,
                    channelGroup = it.channel.channelGroup?.name,
                    startedAt = it.startedAt?.toString(),
                    isRebroadcast = it.rebroadcast ?: false
                )
            }
        } ?: emptyList()

        // Convert related series
        val relatedSeries = workData.seriesList?.nodes?.mapNotNull { series ->
            series?.let {
                val relatedWorks = it.works?.nodes?.mapNotNull { work ->
                    work?.let { w ->
                        RelatedWork(
                            id = w.id,
                            title = w.title,
                            seasonName = w.seasonName?.name,
                            seasonYear = w.seasonYear,
                            media = w.media.name,
                            imageUrl = w.image?.recommendedImageUrl
                        )
                    }
                } ?: emptyList()

                RelatedSeries(
                    id = it.id,
                    name = it.name,
                    nameEn = it.nameEn,
                    nameRo = it.nameRo,
                    works = relatedWorks
                )
            }
        } ?: emptyList()

        AnimeDetailInfo(
            workId = workData.id,
            title = workData.title,
            titleEn = workData.titleEn,
            titleKana = workData.titleKana,
            titleRo = workData.titleRo,
            episodesCount = workData.episodesCount,
            noEpisodes = workData.noEpisodes,
            officialSiteUrl = workData.officialSiteUrl,
            officialSiteUrlEn = workData.officialSiteUrlEn,
            wikipediaUrl = workData.wikipediaUrl,
            wikipediaUrlEn = workData.wikipediaUrlEn,
            twitterHashtag = workData.twitterHashtag,
            twitterUsername = workData.twitterUsername,
            satisfactionRate = workData.satisfactionRate?.toFloat(),
            watchersCount = workData.watchersCount,
            reviewsCount = workData.reviewsCount,
            streamingPlatforms = streamingPlatforms,
            relatedSeries = relatedSeries,
            malEpisodeCount = malData?.numEpisodes,
            malImageUrl = malData?.mainPicture?.large ?: malData?.mainPicture?.medium
        )
    }.onFailure { e ->
        ErrorHandler.handleError(e, "AnimeDetailRepositoryImpl", "getAnimeDetailInfo")
    }
}