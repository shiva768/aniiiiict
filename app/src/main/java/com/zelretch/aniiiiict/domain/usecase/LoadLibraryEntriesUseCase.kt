package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadLibraryEntriesUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    suspend operator fun invoke(states: List<StatusState> = listOf(StatusState.WATCHING)): Flow<List<LibraryEntry>> =
        repository.getLibraryEntries(states)
}
