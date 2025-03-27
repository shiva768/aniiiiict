package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName

data class AnnictChannel(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("started_at")
    val startedAt: String?
) 