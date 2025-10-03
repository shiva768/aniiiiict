package com.zelretch.aniiiiict.integration

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Integration test to verify the MyAnimeList finale judgment works end-to-end
 */
@DisplayName("MyAnimeList最終話判定統合テスト")
class MyAnimeListFinaleJudgmentIntegrationTest {

    private lateinit var repository: MyAnimeListRepository
    private lateinit var useCase: JudgeFinaleUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = JudgeFinaleUseCase(repository)
    }

    @Nested
    @DisplayName("最終話判定")
    inner class FinaleJudgment {

        @Test
        @DisplayName("放送終了アニメは最終話として確認される")
        fun 放送終了アニメは最終話として確認される() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 1,
                mediaType = "tv",
                numEpisodes = 12,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { repository.getAnimeDetail(1) } returns Result.success(media)

            // When
            val result = useCase(12, 1)

            // Then
            assertEquals(FinaleState.FINALE_CONFIRMED, result.state)
            assertTrue(result.isFinale)
        }

        @Test
        @DisplayName("現在のエピソード数が全エピソード数に等しい場合最終話として確認される")
        fun 現在のエピソード数が全エピソード数に等しい場合最終話として確認される() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 2,
                mediaType = "tv",
                numEpisodes = 24,
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { repository.getAnimeDetail(2) } returns Result.success(media)

            // When
            val result = useCase(24, 2)

            // Then
            assertEquals(FinaleState.FINALE_CONFIRMED, result.state)
            assertTrue(result.isFinale)
        }

        @Test
        @DisplayName("現在放送中で最終話でない場合最終話ではないと判定される")
        fun 現在放送中で最終話でない場合最終話ではないと判定される() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 3,
                mediaType = "tv",
                numEpisodes = 12,
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { repository.getAnimeDetail(3) } returns Result.success(media)

            // When
            val result = useCase(8, 3)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }
    }
}
