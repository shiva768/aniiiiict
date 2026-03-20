package com.zelretch.aniiiiict.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkImage

@Entity(tableName = "library_entries")
data class LibraryEntryEntity(
    @PrimaryKey val id: String,
    val workId: String,
    val workTitle: String,
    val workMedia: String?,
    val workSeasonName: String?,
    val workSeasonYear: Int?,
    val workViewerStatusState: String,
    val workMalAnimeId: String?,
    val workNoEpisodes: Boolean,
    val workImageUrl: String?,
    val nextEpisodeId: String?,
    val nextEpisodeNumber: Int?,
    val nextEpisodeNumberText: String?,
    val nextEpisodeTitle: String?,
    val statusState: String?,
    val fetchedAt: Long
)

fun LibraryEntry.toEntity() = LibraryEntryEntity(
    id = id,
    workId = work.id,
    workTitle = work.title,
    workMedia = work.media,
    workSeasonName = work.seasonName?.rawValue,
    workSeasonYear = work.seasonYear,
    workViewerStatusState = work.viewerStatusState.name,
    workMalAnimeId = work.malAnimeId,
    workNoEpisodes = work.noEpisodes,
    workImageUrl = work.image?.imageUrl,
    nextEpisodeId = nextEpisode?.id,
    nextEpisodeNumber = nextEpisode?.number,
    nextEpisodeNumberText = nextEpisode?.numberText,
    nextEpisodeTitle = nextEpisode?.title,
    statusState = statusState?.name,
    fetchedAt = System.currentTimeMillis()
)

fun LibraryEntryEntity.toLibraryEntry() = LibraryEntry(
    id = id,
    work = Work(
        id = workId,
        title = workTitle,
        media = workMedia,
        seasonName = workSeasonName?.let { runCatching { SeasonName.valueOf(it) }.getOrNull() },
        seasonYear = workSeasonYear,
        viewerStatusState = runCatching {
            StatusState.valueOf(workViewerStatusState)
        }.getOrElse { StatusState.UNKNOWN__ },
        malAnimeId = workMalAnimeId,
        noEpisodes = workNoEpisodes,
        image = workImageUrl?.let { WorkImage(recommendedImageUrl = it, facebookOgImageUrl = null) }
    ),
    nextEpisode = nextEpisodeId?.let { epId ->
        Episode(
            id = epId,
            number = nextEpisodeNumber,
            numberText = nextEpisodeNumberText,
            title = nextEpisodeTitle
        )
    },
    statusState = statusState?.let { runCatching { StatusState.valueOf(it) }.getOrNull() }
)
