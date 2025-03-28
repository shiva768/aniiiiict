package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class AnnictProgram(
    val id: Long,
    @SerializedName("started_at")
    val startedAt: LocalDateTime,
    val channel: Channel,
    val work: Work,
    val episode: Episode
)

data class Channel(
    val id: Long,
    val name: String
)

data class Work(
    val id: Long,
    val title: String,
    @SerializedName("media_text")
    val mediaText: String,
    @SerializedName("season_name_text")
    val seasonNameText: String,
    @SerializedName("image_url")
    val imageUrl: String?
)

data class Episode(
    val id: Long,
    val number: Int,
    @SerializedName("number_text")
    val numberText: String,
    val title: String?
) 