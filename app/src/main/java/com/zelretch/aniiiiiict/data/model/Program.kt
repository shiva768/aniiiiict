package com.zelretch.aniiiiiict.data.model

import java.time.LocalDateTime

data class Program(
    val annictId: Int,
    val startedAt: LocalDateTime,
    val channel: Channel,
    val episode: Episode
) 