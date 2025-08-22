package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.Record
import javax.inject.Inject

class SearchRecordsUseCase @Inject constructor() {
    operator fun invoke(records: List<Record>, query: String): List<Record> {
        if (query.isBlank()) return records

        return records.filter { record ->
            val searchableFields = listOf(
                record.work.title,
                record.episode.title,
                record.comment
            )

            searchableFields.any { field ->
                field?.contains(query, ignoreCase = true) == true
            }
        }
    }
}
