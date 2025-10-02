package com.zelretch.aniiiiict.data.model

import com.annict.WorkDetailQuery

data class AnimeDetailInfo(
    // Annict基本情報
    val work: Work,

    // Annict詳細情報
    val programs: List<WorkDetailQuery.Node1?>? = null,
    val seriesList: List<WorkDetailQuery.Node2?>? = null,

    // MyAnimeList情報
    val malInfo: MyAnimeListResponse? = null,

    // 統合情報
    val episodeCount: Int? = null,
    val imageUrl: String? = null,
    val officialSiteUrl: String? = null,
    val wikipediaUrl: String? = null
)
