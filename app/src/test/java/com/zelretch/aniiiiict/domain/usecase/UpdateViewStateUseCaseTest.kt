package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class UpdateViewStateUseCaseTest : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val useCase = UpdateViewStateUseCase(repository)

    given("ステータス更新") {
        `when`("リポジトリがtrueを返す") {
            then("Result.successになる") {
                coEvery { repository.updateWorkViewStatus(any(), any()) } returns true
                val result = runBlocking { useCase("w1", StatusState.WATCHING) }
                result.isSuccess shouldBe true
            }
        }
        `when`("リポジトリがfalseを返す") {
            then("Result.successになる（警告ログ出力）") {
                coEvery { repository.updateWorkViewStatus(any(), any()) } returns false
                val result = runBlocking { useCase("w1", StatusState.WATCHING) }
                result.isSuccess shouldBe true
            }
        }
    }
})
