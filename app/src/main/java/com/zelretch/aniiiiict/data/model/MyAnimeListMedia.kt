package com.zelretch.aniiiiict.data.model

import com.google.gson.annotations.SerializedName

data class MyAnimeListBroadcast(
    @SerializedName("day_of_the_week")
    val dayOfTheWeek: String?,

    val time: String?
)

data class MyAnimeListImage(
    @SerializedName("medium")
    val medium: String?,

    @SerializedName("large")
    val large: String?
)

data class MyAnimeListMainPicture(
    @SerializedName("medium")
    val medium: String?,

    @SerializedName("large")
    val large: String?
)

data class MyAnimeListResponse(
    val id: Int,

    @SerializedName("media_type")
    val mediaType: String?, // "tv", "movie", "ova", etc.

    @SerializedName("num_episodes")
    val numEpisodes: Int?,

    val status: String?, // "finished_airing", "currently_airing", "not_yet_aired"

    val broadcast: MyAnimeListBroadcast?,

    @SerializedName("main_picture")
    val mainPicture: MyAnimeListMainPicture?
)
