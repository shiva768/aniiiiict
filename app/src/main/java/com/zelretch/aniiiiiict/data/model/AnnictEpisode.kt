package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName

data class AnnictEpisode(
    @SerializedName("id")
    val id: Long,

    @SerializedName("number")
    val number: String?,

    @SerializedName("number_text")
    val numberText: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("watched")
    val watched: Boolean
) 