package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class 一括エピソード記録ユースケーステスト : BehaviorSpec({
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val useCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase)

    given("複数エピソードの一括記録") {
        `when`("全て成功する場合") {
            then("Result.successになる") {
                val episodeIds = listOf("ep1", "ep2")
                val workId = "w1"
                val status = StatusState.WANNA_WATCH
                coEvery { watchEpisodeUseCase(any(), any(), any(), any()) } returns Result.success(Unit)
                val result = runBlocking { useCase(episodeIds, workId, status) }
                result.isSuccess shouldBe true
            }
        }
        `when`("途中で失敗する場合") {
            then("Result.failureになる") {
                val episodeIds = listOf("ep1", "ep2")
                val workId = "w1"
                val status = StatusState.WANNA_WATCH
                coEvery { watchEpisodeUseCase("ep1", workId, status, true) } returns Result.success(Unit)
                coEvery { watchEpisodeUseCase("ep2", workId, status, false) } returns Result.failure(Exception("fail"))
                val result = runBlocking { useCase(episodeIds, workId, status) }
                result.isFailure shouldBe true
            }
        }
    }
})
