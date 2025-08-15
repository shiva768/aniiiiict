package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import javax.inject.Inject

class FilterProgramsUseCase
    @Inject
    constructor(
        private val programFilter: ProgramFilter,
    ) {
        operator fun invoke(
            programs: List<ProgramWithWork>,
            filterState: FilterState,
        ): List<ProgramWithWork> = programFilter.applyFilters(programs, filterState)

        fun extractAvailableFilters(programs: List<ProgramWithWork>) = programFilter.extractAvailableFilters(programs)
    }
