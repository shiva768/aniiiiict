package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.Record
import javax.inject.Inject

class SearchRecordsUseCase
    @Inject
    constructor() {
        operator fun invoke(
            records: List<Record>,
            query: String,
        ): List<Record> {
            if (query.isBlank()) return records

            return records.filter { record ->
                record.work.title.contains(query, ignoreCase = true) ||
                    record.episode.title?.contains(query, ignoreCase = true) == true ||
                    record.comment?.contains(query, ignoreCase = true) == true
            }
        }
    }
