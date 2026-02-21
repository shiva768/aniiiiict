package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(private val repository: AnnictRepository) {
    suspend operator fun invoke(recordId: String): Result<Unit> = repository.deleteRecord(recordId)
}
