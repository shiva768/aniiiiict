package com.zelretch.aniiiiict.data.model

data class StreamingPlatform(
    val id: String,
    val name: String,
    val channelGroup: String?,
    val startedAt: String?,
    val isRebroadcast: Boolean = false
)

data class RelatedWork(
    val id: String,
    val title: String,
    val seasonName: String?,
    val seasonYear: Int?,
    val media: String?,
    val imageUrl: String?
)

data class RelatedSeries(
    val id: String,
    val name: String,
    val nameEn: String?,
    val nameRo: String?,
    val works: List<RelatedWork>
)

data class AnimeDetailInfo(
    val workId: String,
    val title: String,
    val titleEn: String?,
    val titleKana: String?,
    val titleRo: String?,
    val episodesCount: Int?,
    val noEpisodes: Boolean,
    val officialSiteUrl: String?,
    val officialSiteUrlEn: String?,
    val wikipediaUrl: String?,
    val wikipediaUrlEn: String?,
    val twitterHashtag: String?,
    val twitterUsername: String?,
    val satisfactionRate: Float?,
    val watchersCount: Int?,
    val reviewsCount: Int?,
    val streamingPlatforms: List<StreamingPlatform>,
    val relatedSeries: List<RelatedSeries>,
    // MyAnimeList specific fields
    val malEpisodeCount: Int?,
    val malImageUrl: String?
)
