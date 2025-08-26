package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(private val repository: AnnictRepository) {
    suspend operator fun invoke(recordId: String): Boolean = repository.deleteRecord(recordId)
}
