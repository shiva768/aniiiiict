package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class LoadProgramsUseCaseTest : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = LoadProgramsUseCase(repository)

    given("プログラムがリポジトリに存在する場合") {
        `when`("invokeを呼ぶ") {
            then("リポジトリの値がそのまま返る") {
                val mockPrograms = listOf<ProgramWithWork>()
                coEvery { repository.getProgramsWithWorks() } returns flow { emit(mockPrograms) }
                val result = useCase().first()
                result shouldBe mockPrograms
            }
        }
    }
})
