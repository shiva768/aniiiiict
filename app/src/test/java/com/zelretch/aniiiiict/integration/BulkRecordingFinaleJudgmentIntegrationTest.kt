package com.zelretch.aniiiiict.integration

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleJudgmentInfo
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration test to verify bulk recording with finale judgment works end-to-end
 */
@DisplayName("バルク記録でのフィナーレ判定統合テスト")
class BulkRecordingFinaleJudgmentIntegrationTest {

    private lateinit var annictRepository: AnnictRepository
    private lateinit var malRepository: MyAnimeListRepository
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var bulkRecordUseCase: BulkRecordEpisodesUseCase

    @BeforeEach
    fun setup() {
        annictRepository = mockk()
        malRepository = mockk()
        updateViewStateUseCase = mockk()

        judgeFinaleUseCase = JudgeFinaleUseCase(malRepository)
        watchEpisodeUseCase = WatchEpisodeUseCase(annictRepository, updateViewStateUseCase)
        bulkRecordUseCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase, judgeFinaleUseCase)
    }

    @Nested
    @DisplayName("フィナーレ判定")
    inner class FinaleJudgment {

        @Test
        @DisplayName("最終話を含むエピソードをバルク記録するとフィナーレ判定が実行される")
        fun withFinaleEpisode() = runTest {
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
                broadcast = null,
                mainPicture = null
            )

            coEvery { annictRepository.createRecord(any(), any()) } returns Result.success(Unit)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { malRepository.getAnimeDetail(malAnimeId) } returns Result.success(media)

            // Act
            val finaleInfo = FinaleJudgmentInfo(
                malAnimeId = malAnimeId,
                lastEpisodeNumber = lastEpisodeNumber,
                lastEpisodeHasNext = null
            )
            val result = bulkRecordUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING,
                finaleInfo = finaleInfo
            )

            // Assert
            assertTrue(result.isSuccess)
            val bulkResult = result.getOrNull()!!
            assertEquals(FinaleState.FINALE_CONFIRMED, bulkResult.finaleResult?.state)
            assertTrue(bulkResult.finaleResult?.isFinale == true)
        }

        @Test
        @DisplayName("最終話でないエピソードをバルク記録すると非最終話として判定される")
        fun withNonFinaleEpisode() = runTest {
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
                broadcast = null,
                mainPicture = null
            )

            coEvery { annictRepository.createRecord(any(), any()) } returns Result.success(Unit)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
            coEvery { malRepository.getAnimeDetail(malAnimeId) } returns Result.success(media)

            // Act
            val finaleInfo = FinaleJudgmentInfo(
                malAnimeId = malAnimeId,
                lastEpisodeNumber = lastEpisodeNumber,
                lastEpisodeHasNext = null
            )
            val result = bulkRecordUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING,
                finaleInfo = finaleInfo
            )

            // Assert
            assertTrue(result.isSuccess)
            val bulkResult = result.getOrNull()!!
            assertEquals(FinaleState.NOT_FINALE, bulkResult.finaleResult?.state)
            assertFalse(bulkResult.finaleResult?.isFinale == true)
        }

        @Test
        @DisplayName("MAL IDがない場合フィナーレ判定は実行されない")
        fun MAL_IDがない場合フィナーレ判定は実行されない() = runTest {
            // Arrange
            val episodeIds = listOf("ep1", "ep2")
            val workId = "work1"

            coEvery { annictRepository.createRecord(any(), any()) } returns Result.success(Unit)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)

            // Act
            val result = bulkRecordUseCase(
                episodeIds = episodeIds,
                workId = workId,
                currentStatus = StatusState.WATCHING
                // malAnimeId and lastEpisodeNumber are null
            )

            // Assert
            assertTrue(result.isSuccess)
            val bulkResult = result.getOrNull()!!
            assertNull(bulkResult.finaleResult)
        }
    }
}
