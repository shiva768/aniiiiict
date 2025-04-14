package com.zelretch.aniiiiiict.data.model

import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState

data class Work(
    val id: String,
    val annictId: Long = 0,
    val title: String,
    val seasonName: SeasonName? = null,
    val seasonYear: Int? = null,
    val media: String? = null,
    val mediaText: String? = null,
    val viewerStatusState: StatusState,
    val seasonNameText: String? = null,
    val image: WorkImage? = null,
    val imageUrl: String? = null
) 