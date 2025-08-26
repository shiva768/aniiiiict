package com.zelretch.aniiiiict.data.model

import java.time.ZonedDateTime

data class Record(
    val id: String,
    val comment: String?,
    val rating: Double?,
    val createdAt: ZonedDateTime,
    val episode: Episode,
    val work: Work
)
