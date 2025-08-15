package com.zelretch.aniiiiiict.data.model

data class AniListMedia(
    val id: Int,
    val format: String?,
    val episodes: Int?,
    val status: String?,
    val nextAiringEpisode: NextAiringEpisode?,
)
