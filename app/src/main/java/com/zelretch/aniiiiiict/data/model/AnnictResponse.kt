package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName

data class AnnictResponse<T>(
    @SerializedName("works")
    val works: List<T>
) 