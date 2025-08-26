package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import javax.inject.Inject

class FilterProgramsUseCase @Inject constructor(private val programFilter: ProgramFilter) {
    operator fun invoke(programs: List<ProgramWithWork>, filterState: FilterState): List<ProgramWithWork> =
        programFilter.applyFilters(programs, filterState)

    fun extractAvailableFilters(programs: List<ProgramWithWork>) = programFilter.extractAvailableFilters(programs)
}
