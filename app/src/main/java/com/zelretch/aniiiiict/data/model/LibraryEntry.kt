package com.zelretch.aniiiiict.data.model

data class LibraryEntry(
    val id: String,
    val work: Work,
    val nextEpisode: Episode?,
    val statusState: com.annict.type.StatusState?,
    // 端末内だけで持つ優先度（Annict には無い）
    val priority: WorkPriority = WorkPriority.NONE
)
