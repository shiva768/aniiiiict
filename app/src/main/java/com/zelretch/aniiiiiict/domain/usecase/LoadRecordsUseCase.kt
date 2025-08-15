package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import javax.inject.Inject

data class RecordsResult(
    val records: List<Record>,
    val hasNextPage: Boolean,
    val endCursor: String?,
)

class LoadRecordsUseCase
    @Inject
    constructor(
        private val repository: AnnictRepository,
    ) {
        suspend operator fun invoke(cursor: String? = null): RecordsResult {
            val result = repository.getRecords(cursor)
            return RecordsResult(
                records = result.records,
                hasNextPage = result.hasNextPage,
                endCursor = result.endCursor,
            )
        }
    }
