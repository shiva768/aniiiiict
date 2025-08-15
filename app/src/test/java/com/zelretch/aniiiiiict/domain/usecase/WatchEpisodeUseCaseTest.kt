package com.zelretch.aniiiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class WatchEpisodeUseCaseTest :
    BehaviorSpec({
        val repository = mockk<AnnictRepository>()
        val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
        val useCase = WatchEpisodeUseCase(repository, updateViewStateUseCase)

        given("エピソード視聴記録") {
            `when`("記録が成功し、ステータスも更新される場合") {
                then("Result.successになる") {
                    coEvery { repository.createRecord(any(), any()) } returns true
                    coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
                    val result = runBlocking { useCase("ep1", "w1", StatusState.WANNA_WATCH, true) }
                    result.isSuccess shouldBe true
                }
            }
            `when`("記録が失敗する場合") {
                then("Result.failureになる") {
                    coEvery { repository.createRecord(any(), any()) } returns false
                    val result = runBlocking { useCase("ep1", "w1", StatusState.WANNA_WATCH, true) }
                    result.isFailure shouldBe true
                }
            }
        }
    })
