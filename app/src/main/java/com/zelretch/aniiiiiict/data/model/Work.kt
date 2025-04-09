package com.zelretch.aniiiiiict.data.model

data class Work(
    val id: String? = null,
    val annictId: Long = 0,
    val title: String,
    val seasonName: String? = null,
    val seasonYear: Int? = null,
    val media: String? = null,
    val mediaText: String? = null,
    val viewerStatusState: String? = null,
    val seasonNameText: String? = null,
    val image: WorkImage? = null,
    val imageUrl: String? = null
) 