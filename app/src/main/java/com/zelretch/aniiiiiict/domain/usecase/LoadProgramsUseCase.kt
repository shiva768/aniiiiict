package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadProgramsUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    operator fun invoke(): Flow<List<ProgramWithWork>> {
        return repository.getProgramsWithWorks()
    }
} 