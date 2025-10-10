package com.zelretch.aniiiiict.data.model

data class LibraryEntry(
    val id: String,
    val work: Work,
    val nextEpisode: Episode?,
    val statusState: com.annict.type.StatusState?
)
