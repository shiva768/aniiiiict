package com.zelretch.aniiiiict.data.model

import com.annict.WorkDetailQuery
import com.annict.WorkSeriesListQuery

data class AnimeDetailInfo(
    // Annict基本情報
    val work: Work,

    // Annict詳細情報
    val programs: List<WorkDetailQuery.Node1?>? = null,
    val casts: List<WorkDetailQuery.Node2?>? = null,
    val seriesList: List<WorkSeriesListQuery.Node1?>? = null,

    // MyAnimeList情報
    val malInfo: MyAnimeListResponse? = null,

    // 統合情報
    val episodeCount: Int? = null,
    val imageUrl: String? = null,
    val officialSiteUrl: String? = null,
    val wikipediaUrl: String? = null
)
