package com.zelretch.aniiiiiict.domain.usecase

import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class DeleteRecordUseCaseTest : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = DeleteRecordUseCase(repository)

    given("レコード削除") {
        `when`("repositoryがtrueを返す場合") {
            then("trueが返る") {
                coEvery { repository.deleteRecord("record1") } returns true
                val result = runBlocking { useCase("record1") }
                result shouldBe true
            }
        }
        `when`("repositoryがfalseを返す場合") {
            then("falseが返る") {
                coEvery { repository.deleteRecord("record2") } returns false
                val result = runBlocking { useCase("record2") }
                result shouldBe false
            }
        }
    }
})
