package com.zelretch.aniiiiict.integration

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Integration test to verify bulk recording with finale judgment works end-to-end
 */
class BulkRecordingFinaleJudgmentIntegrationTest : BehaviorSpec({

    given("バルク記録でのフィナーレ判定統合テスト") {
        val annictRepository = mockk<AnnictRepository>()
        val malRepository = mockk<MyAnimeListRepository>()
        val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()

        val judgeFinaleUseCase = JudgeFinaleUseCase(malRepository)
        val watchEpisodeUseCase = WatchEpisodeUseCase(annictRepository, updateViewStateUseCase)
        val bulkRecordUseCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase, judgeFinaleUseCase)

        `when`("最終話を含むエピソードをバルク記録する") {
            then("フィナーレ判定が実行され、結果が返される") {
                // Arrange
                val episodeIds = listOf("ep11", "ep12")
                val workId = "work1"
                val malAnimeId = 123
                val lastEpisodeNumber = 12
                val media = MyAnimeListResponse(
                    id = malAnimeId,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "finished_airing",
                    broadcast = null
                )

                coEvery { annictRepository.createRecord(any(), any()) } returns true
                coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
                coEvery { malRepository.getAnimeDetail(malAnimeId) } returns Result.success(media)

                // Act
                val result = bulkRecordUseCase(
                    episodeIds = episodeIds,
                    workId = workId,
                    currentStatus = StatusState.WATCHING,
                    malAnimeId = malAnimeId,
                    lastEpisodeNumber = lastEpisodeNumber
                )

                // Assert
                result.isSuccess shouldBe true
                val bulkResult = result.getOrNull()!!
                bulkResult.finaleResult?.state shouldBe FinaleState.FINALE_CONFIRMED
                bulkResult.finaleResult?.isFinale shouldBe true
            }
        }

        `when`("最終話ではないエピソードをバルク記録する") {
            then("フィナーレ判定が実行され、非最終話として判定される") {
                // Arrange
                val episodeIds = listOf("ep8", "ep9")
                val workId = "work1"
                val malAnimeId = 123
                val lastEpisodeNumber = 9
                val media = MyAnimeListResponse(
                    id = malAnimeId,
                    mediaType = "tv",
                    numEpisodes = 12,
                    status = "currently_airing",
                    broadcast = null
                )

                coEvery { annictRepository.createRecord(any(), any()) } returns true
                coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
                coEvery { malRepository.getAnimeDetail(malAnimeId) } returns Result.success(media)

                // Act
                val result = bulkRecordUseCase(
                    episodeIds = episodeIds,
                    workId = workId,
                    currentStatus = StatusState.WATCHING,
                    malAnimeId = malAnimeId,
                    lastEpisodeNumber = lastEpisodeNumber
                )

                // Assert
                result.isSuccess shouldBe true
                val bulkResult = result.getOrNull()!!
                bulkResult.finaleResult?.state shouldBe FinaleState.NOT_FINALE
                bulkResult.finaleResult?.isFinale shouldBe false
            }
        }

        `when`("MAL IDがない場合") {
            then("フィナーレ判定は実行されず、nullが返される") {
                // Arrange
                val episodeIds = listOf("ep1", "ep2")
                val workId = "work1"

                coEvery { annictRepository.createRecord(any(), any()) } returns true
                coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)

                // Act
                val result = bulkRecordUseCase(
                    episodeIds = episodeIds,
                    workId = workId,
                    currentStatus = StatusState.WATCHING
                    // malAnimeId and lastEpisodeNumber are null
                )

                // Assert
                result.isSuccess shouldBe true
                val bulkResult = result.getOrNull()!!
                bulkResult.finaleResult shouldBe null
            }
        }
    }
})
