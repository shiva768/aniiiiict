package com.zelretch.aniiiiict.domain.usecase

import com.annict.ViewerProgramsQuery
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkImage as WorkImageModel
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.ui.base.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class LoadProgramsUseCase @Inject constructor(private val repository: AnnictRepository) {
    suspend operator fun invoke(): Flow<List<ProgramWithWork>> = repository.getRawProgramsData().map { rawPrograms ->
        processProgramsResponse(rawPrograms)
    }

    private fun processProgramsResponse(responsePrograms: List<ViewerProgramsQuery.Node?>): List<ProgramWithWork> {
        val programs = responsePrograms.mapNotNull { node ->
            if (node == null) return@mapNotNull null
            val startedAt = try {
                // Parse the UTC datetime string to ZonedDateTime
                val utcDateTime = ZonedDateTime.parse(
                    node.startedAt.toString(),
                    DateTimeFormatter.ISO_DATE_TIME
                )
                // Convert to JST timezone
                val jstDateTime = utcDateTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                // Convert to LocalDateTime
                jstDateTime.toLocalDateTime()
            } catch (e: Exception) {
                ErrorHandler.handleError(e, "LoadProgramsUseCase", "processProgramsResponse")
                LocalDateTime.now() // パースに失敗した場合は現在時刻を使用
            }

            val channel = Channel(
                name = node.channel.name
            )

            val episode = Episode(
                id = node.episode.id,
                number = node.episode.number,
                numberText = node.episode.numberText,
                title = node.episode.title
            )

            val workImage = node.work.image?.let { image ->
                WorkImageModel(
                    recommendedImageUrl = image.recommendedImageUrl,
                    facebookOgImageUrl = image.facebookOgImageUrl
                )
            }

            val work = Work(
                id = node.work.id,
                title = node.work.title,
                seasonName = node.work.seasonName,
                seasonYear = node.work.seasonYear,
                media = node.work.media.toString(),
                malAnimeId = node.work.malAnimeId,
                viewerStatusState = node.work.viewerStatusState ?: StatusState.UNKNOWN__,
                image = workImage,
                wikipediaUrl = node.work.wikipediaUrl,
                wikipediaUrlEn = node.work.wikipediaUrlEn,
                officialSiteUrl = node.work.officialSiteUrl,
                officialSiteUrlEn = node.work.officialSiteUrlEn,
                syobocalTid = node.work.syobocalTid
            )

            val program = Program(
                id = node.id,
                startedAt = startedAt,
                channel = channel,
                episode = episode
            )

            program to work
        }

        // 各作品のプログラムをすべて保持し、最初のエピソードも特定する
        return programs.groupBy { it.second.title }.map { (_, grouped) ->
            val sortedPrograms = grouped.sortedBy { it.first.episode.number ?: Int.MAX_VALUE }
            val firstProgram = sortedPrograms.firstOrNull()!!
            ProgramWithWork(
                programs = sortedPrograms.map { it.first },
                firstProgram = firstProgram.first,
                work = firstProgram.second
            )
        }.sortedBy { it.firstProgram.startedAt }
    }
}
