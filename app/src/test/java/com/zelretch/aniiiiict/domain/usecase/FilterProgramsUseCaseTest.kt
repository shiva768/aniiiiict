package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.SeasonName
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.filter.createProgramWithWork
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FilterProgramsUseCase")
class FilterProgramsUseCaseTest {

    private lateinit var filterProgramsUseCase: FilterProgramsUseCase

    @BeforeEach
    fun setup() {
        filterProgramsUseCase = FilterProgramsUseCase(ProgramFilter())
    }

    @Nested
    @DisplayName("シーズンフィルター")
    inner class SeasonFilter {

        @Test
        @DisplayName("指定したシーズンのみ返る")
        fun withSelectedSeasons() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonName = SeasonName.WINTER),
                createProgramWithWork(seasonName = SeasonName.SUMMER)
            )
            val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))

            // When
            val result = filterProgramsUseCase(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(SeasonName.WINTER, result[0].work.seasonName)
        }
    }
}
