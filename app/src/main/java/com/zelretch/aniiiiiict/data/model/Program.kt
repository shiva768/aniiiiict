package com.zelretch.aniiiiiict.data.model

import java.time.LocalDateTime

data class Program(
    val id: String,
    val startedAt: LocalDateTime,
    val channel: Channel,
    val episode: Episode
)
