package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class BulkRecordEpisodesUseCaseTest : BehaviorSpec({
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()
    val useCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase, judgeFinaleUseCase)

    given("複数エピソードの一括記録") {
        `when`("全て成功し、フィナーレ情報なしの場合") {
            then("Result.successになり、finaleResultはnull") {
                val episodeIds = listOf("ep1", "ep2")
                val workId = "w1"
                val status = StatusState.WANNA_WATCH
                coEvery { watchEpisodeUseCase(any(), any(), any(), any()) } returns Result.success(Unit)
                
                val result = runBlocking { useCase(episodeIds, workId, status) }
                
                result.isSuccess shouldBe true
                result.getOrNull()?.finaleResult shouldBe null
            }
        }

        `when`("全て成功し、フィナーレ判定ありの場合") {
            then("Result.successになり、finaleResultが含まれる") {
                val episodeIds = listOf("ep1", "ep2")
                val workId = "w1"
                val status = StatusState.WANNA_WATCH
                val malAnimeId = 123
                val lastEpisodeNumber = 12
                val finaleResult = JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)
                
                coEvery { watchEpisodeUseCase(any(), any(), any(), any()) } returns Result.success(Unit)
                coEvery { judgeFinaleUseCase(lastEpisodeNumber, malAnimeId) } returns finaleResult
                
                val result = runBlocking { 
                    useCase(episodeIds, workId, status, malAnimeId, lastEpisodeNumber)
                }
                
                result.isSuccess shouldBe true
                result.getOrNull()?.finaleResult shouldBe finaleResult
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

        `when`("空のエピソードリストの場合") {
            then("空のResultが返される") {
                val episodeIds = emptyList<String>()
                val workId = "w1"
                val status = StatusState.WANNA_WATCH
                
                val result = runBlocking { useCase(episodeIds, workId, status) }
                
                result.isSuccess shouldBe true
                result.getOrNull()?.finaleResult shouldBe null
            }
        }
    }
})
