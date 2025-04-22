package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    suspend operator fun invoke(recordId: String): Boolean {
        return repository.deleteRecord(recordId)
    }
}
