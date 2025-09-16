package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.SeasonName
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.filter.createProgramWithWork
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class プログラムフィルターユースケーステスト : BehaviorSpec({
    val filterProgramsUseCase = FilterProgramsUseCase(ProgramFilter())

    前提("複数のProgramWithWorkがあるとき") {
        場合("シーズンでフィルタする") {
            そのとき("指定したシーズンのみ返る") {
                val programs = listOf(
                    createProgramWithWork(seasonName = SeasonName.WINTER),
                    createProgramWithWork(seasonName = SeasonName.SUMMER)
                )
                val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))
                val result = filterProgramsUseCase(programs, filterState)
                result.size shouldBe 1
                result[0].work.seasonName shouldBe SeasonName.WINTER
            }
        }
    }
})
