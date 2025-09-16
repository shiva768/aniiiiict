package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class エピソード視聴ユースケーステスト : BehaviorSpec({
    val repository = mockk<AnnictRepository>()
    val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
    val useCase = WatchEpisodeUseCase(repository, updateViewStateUseCase)

    前提("エピソード視聴記録") {
        場合("記録が成功し、ステータスも更新される場合") {
            そのとき("Result.successになる") {
                coEvery { repository.createRecord(any(), any()) } returns true
                coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
                val result = runBlocking { useCase("ep1", "w1", StatusState.WANNA_WATCH, true) }
                result.isSuccess shouldBe true
            }
        }
        場合("記録が失敗する場合") {
            そのとき("Result.failureになる") {
                coEvery { repository.createRecord(any(), any()) } returns false
                val result = runBlocking { useCase("ep1", "w1", StatusState.WANNA_WATCH, true) }
                result.isFailure shouldBe true
            }
        }
    }
})
