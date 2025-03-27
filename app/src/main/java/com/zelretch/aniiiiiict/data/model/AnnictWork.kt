package com.zelretch.aniiiiiict.data.model

import com.google.gson.annotations.SerializedName

data class AnnictWork(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("title_kana")
    val titleKana: String?,

    @SerializedName("media")
    val media: String,

    @SerializedName("media_text")
    val mediaText: String,

    @SerializedName("season_name")
    val seasonName: String?,

    @SerializedName("season_name_text")
    val seasonNameText: String?,

    @SerializedName("season_year")
    val seasonYear: Int?,

    @SerializedName("season_year_text")
    val seasonYearText: String?,

    @SerializedName("released_on")
    val releasedOn: String?,

    @SerializedName("released_on_about")
    val releasedOnAbout: String?,

    @SerializedName("official_site_url")
    val officialSiteUrl: String?,

    @SerializedName("wikipedia_url")
    val wikipediaUrl: String?,

    @SerializedName("twitter_username")
    val twitterUsername: String?,

    @SerializedName("twitter_hashtag")
    val twitterHashtag: String?,

    @SerializedName("syobocal_tid")
    val syobocalTid: String?,

    @SerializedName("mal_anime_id")
    val malAnimeId: String?,

    @SerializedName("images")
    val images: Images?,

    @SerializedName("episodes_count")
    val episodesCount: Int,

    @SerializedName("watchers_count")
    val watchersCount: Int,

    @SerializedName("reviews_count")
    val reviewsCount: Int,

    @SerializedName("no_episodes")
    val noEpisodes: Boolean,

    @SerializedName("status")
    val status: Status?,

    @SerializedName("channels")
    val channels: List<AnnictChannel>?,

    @SerializedName("episodes")
    val episodes: List<AnnictEpisode>?
) {
    data class Status(
        @SerializedName("kind")
        val kind: String
    )

    data class Images(
        @SerializedName("recommended_url")
        val recommendedUrl: String?,

        @SerializedName("facebook")
        val facebook: Facebook?,

        @SerializedName("twitter")
        val twitter: Twitter?
    ) {
        data class Facebook(
            @SerializedName("og_image_url")
            val ogImageUrl: String?
        )

        data class Twitter(
            @SerializedName("mini_avatar_url")
            val miniAvatarUrl: String?,

            @SerializedName("normal_avatar_url")
            val normalAvatarUrl: String?,

            @SerializedName("bigger_avatar_url")
            val biggerAvatarUrl: String?,

            @SerializedName("original_avatar_url")
            val originalAvatarUrl: String?,

            @SerializedName("image_url")
            val imageUrl: String?
        )
    }

    val nextUnwatchedEpisode: AnnictEpisode?
        get() = episodes?.firstOrNull { !it.watched }
}
