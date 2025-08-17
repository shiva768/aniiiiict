package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class MyActivity(
    val record: Record,
    val genres: List<String>,
    val studios: List<String>
)

class GetMyActivitiesUseCase @Inject constructor(
    private val annictRepository: AnnictRepository,
    private val aniListRepository: AniListRepository
) {
    operator fun invoke(): Flow<List<MyActivity>> = flow {
        val allRecords = mutableListOf<Record>()
        var cursor: String? = null
        var hasNextPage = true

        while (hasNextPage) {
            val paginatedResult = annictRepository.getRecords(after = cursor)
            allRecords.addAll(paginatedResult.records)
            hasNextPage = paginatedResult.hasNextPage
            cursor = paginatedResult.endCursor
        }

        val myActivities = allRecords.mapNotNull { record ->
            try {
                val anilistId = record.work.id.toInt()
                val mediaResult = aniListRepository.getMedia(anilistId)
                mediaResult.getOrNull()?.let { media ->
                    MyActivity(
                        record = record,
                        genres = media.genres ?: emptyList(),
                        studios = media.studios ?: emptyList()
                    )
                }
            } catch (e: NumberFormatException) {
                // Annict work ID might not be a valid integer for Anilist, so we skip it.
                null
            }
        }
        emit(myActivities)
    }
}
