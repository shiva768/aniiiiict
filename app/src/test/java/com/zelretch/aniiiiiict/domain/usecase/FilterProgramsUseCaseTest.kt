package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.SeasonName
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiiict.domain.filter.createProgramWithWork
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class FilterProgramsUseCaseTest :
    BehaviorSpec({
        val filterProgramsUseCase = FilterProgramsUseCase(ProgramFilter())

        given("複数のProgramWithWorkがあるとき") {
            `when`("シーズンでフィルタする") {
                then("指定したシーズンのみ返る") {
                    val programs =
                        listOf(
                            createProgramWithWork(seasonName = SeasonName.WINTER),
                            createProgramWithWork(seasonName = SeasonName.SUMMER),
                        )
                    val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))
                    val result = filterProgramsUseCase(programs, filterState)
                    result.size shouldBe 1
                    result[0].work.seasonName shouldBe SeasonName.WINTER
                }
            }
        }
    })
