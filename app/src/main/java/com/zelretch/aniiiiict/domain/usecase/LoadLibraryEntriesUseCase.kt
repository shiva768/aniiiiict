package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntriesPage
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import javax.inject.Inject

private val ALL_STATUSES = listOf(
    StatusState.WATCHING,
    StatusState.WANNA_WATCH,
    StatusState.WATCHED,
    StatusState.ON_HOLD,
    StatusState.STOP_WATCHING
)

class LoadLibraryEntriesUseCase @Inject constructor(
    private val repository: AnnictRepository
) {
    suspend operator fun invoke(
        states: List<StatusState> = ALL_STATUSES,
        after: String? = null
    ): Result<LibraryEntriesPage> = repository.getLibraryEntries(states, after)
}
