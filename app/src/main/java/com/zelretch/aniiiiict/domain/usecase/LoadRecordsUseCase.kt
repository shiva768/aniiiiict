package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject

class LoadRecordsUseCase @Inject constructor(private val repository: AnnictRepository) {
    suspend operator fun invoke(cursor: String? = null): Result<PaginatedRecords> = repository.getRecords(cursor)
}
