package com.zelretch.aniiiiict.domain.usecase

import com.annict.type.StatusState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BulkRecordEpisodesUseCase")
class BulkRecordEpisodesUseCaseTest {

    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var useCase: BulkRecordEpisodesUseCase

    @BeforeEach
    fun setup() {
        watchEpisodeUseCase = mockk()
        judgeFinaleUseCase = mockk()
        useCase = BulkRecordEpisodesUseCase(watchEpisodeUseCase, judgeFinaleUseCase)
    }

    @Nested
    @DisplayName("複数エピソードの一括記録")
    inner class BulkRecord {

        @Test
        @DisplayName("全て成功しフィナーレ情報なしの場合Result.successでfinaleResultはnull")
        fun withoutFinale() = runTest {
            // Given
            val episodeIds = listOf("ep1", "ep2")
            val workId = "w1"
            val status = StatusState.WANNA_WATCH
            coEvery { watchEpisodeUseCase(any(), any(), any(), any()) } returns Result.success(Unit)

            // When
            val result = useCase(episodeIds, workId, status)

            // Then
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull()?.finaleResult)
        }

        @Test
        @DisplayName("全て成功しフィナーレ判定ありの場合Result.successでfinaleResultが含まれる")
        fun withFinale() = runTest {
            // Given
            val episodeIds = listOf("ep1", "ep2")
            val workId = "w1"
            val status = StatusState.WANNA_WATCH
            val malAnimeId = 123
            val lastEpisodeNumber = 12
            val finaleResult = JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)

            coEvery { watchEpisodeUseCase(any(), any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase(lastEpisodeNumber, malAnimeId) } returns finaleResult

            // When
            val finaleInfo = FinaleJudgmentInfo(
                malAnimeId = malAnimeId,
                lastEpisodeNumber = lastEpisodeNumber,
                lastEpisodeHasNext = null
            )
            val result = useCase(episodeIds, workId, status, finaleInfo)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(finaleResult, result.getOrNull()?.finaleResult)
        }

        @Test
        @DisplayName("途中で失敗する場合Result.failureを返す")
        fun onFailure() = runTest {
            // Given
            val episodeIds = listOf("ep1", "ep2")
            val workId = "w1"
            val status = StatusState.WANNA_WATCH
            coEvery { watchEpisodeUseCase("ep1", workId, status, true) } returns Result.success(Unit)
            coEvery { watchEpisodeUseCase("ep2", workId, status, false) } returns Result.failure(Exception("fail"))

            // When
            val result = useCase(episodeIds, workId, status)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("空のエピソードリストの場合空のResultが返される")
        fun withEmptyList() = runTest {
            // Given
            val episodeIds = emptyList<String>()
            val workId = "w1"
            val status = StatusState.WANNA_WATCH

            // When
            val result = useCase(episodeIds, workId, status)

            // Then
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull()?.finaleResult)
        }
    }
}
