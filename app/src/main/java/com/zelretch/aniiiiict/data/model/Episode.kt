package com.zelretch.aniiiiict.data.model

data class Episode(
    val id: String,
    val number: Int? = null,
    val numberText: String? = null,
    val title: String? = null,
    val viewerDidTrack: Boolean? = null
) {
    val formattedNumber: String get() = numberText ?: "第${number}話"
}
