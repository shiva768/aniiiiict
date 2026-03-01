package com.zelretch.aniiiiict.domain.usecase

import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
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

@DisplayName("JudgeFinaleUseCase")
class JudgeFinaleUseCaseTest {

    private lateinit var myAnimeListRepository: MyAnimeListRepository
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase

    @BeforeEach
    fun setup() {
        myAnimeListRepository = mockk()
        judgeFinaleUseCase = JudgeFinaleUseCase(myAnimeListRepository)
    }

    @Nested
    @DisplayName("最終話判定ロジック")
    inner class FinaleJudgment {

        @Test
        @DisplayName("hasNextEpisode=trueの場合はnot_finaleを返す（最優先）")
        fun hasNextEpisodeTrueの場合はNot_finaleを返す() = runTest {
            // Given: numEpisodesに達していてもhasNextEpisode=trueなら最終話ではない（1期・2期問題）
            val media = MyAnimeListResponse(
                id = 1,
                mediaType = "tv",
                numEpisodes = 12,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(12, media.id, hasNextEpisode = true)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("currently_airingの場合はnot_finaleを返す")
        fun currently_airingの場合はNot_finaleを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 2,
                mediaType = "tv",
                numEpisodes = 12,
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When: numEpisodesに達していても放送中なら最終話ではない
            val result = judgeFinaleUseCase(12, media.id)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("currently_airingかつnumEpisodesがnullでhasNextEpisode=falseの場合はnot_finaleを返す")
        fun currently_airingかつnumEpisodesがnullでhasNextEpisodeFalseの場合はNot_finaleを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 3,
                mediaType = "tv",
                numEpisodes = null,
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(10, media.id, hasNextEpisode = false)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("currently_airingかつnumEpisodesがnullでhasNextEpisodeがnullの場合はnot_finaleを返す")
        fun currently_airingかつnumEpisodesがnullでhasNextEpisodeNullの場合はNot_finaleを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 4,
                mediaType = "tv",
                numEpisodes = null,
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(10, media.id, hasNextEpisode = null)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("numEpisodesに達しstatusがfinished_airingの場合はfinale_confirmedを返す")
        fun numEpisodesに達しstatusがfinishedAiringの場合はFinale_confirmedを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 5,
                mediaType = "tv",
                numEpisodes = 12,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(12, media.id)

            // Then
            assertEquals(FinaleState.FINALE_CONFIRMED, result.state)
            assertTrue(result.isFinale)
        }

        @Test
        @DisplayName("movie_numEpisodesに達した場合はfinale_confirmedを返す")
        fun movie_numEpisodesに達した場合はfinale_confirmedを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 6,
                mediaType = "movie",
                numEpisodes = 1,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(1, media.id)

            // Then
            assertEquals(FinaleState.FINALE_CONFIRMED, result.state)
            assertTrue(result.isFinale)
        }

        @Test
        @DisplayName("ova_numEpisodesに達した場合はfinale_confirmedを返す")
        fun ova_numEpisodesに達した場合はfinale_confirmedを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 7,
                mediaType = "ova",
                numEpisodes = 6,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(6, media.id)

            // Then
            assertEquals(FinaleState.FINALE_CONFIRMED, result.state)
            assertTrue(result.isFinale)
        }

        @Test
        @DisplayName("numEpisodesがnullでhasNextEpisode=trueの場合はnot_finaleを返す")
        fun numEpisodesがnullでhasNextEpisodeTrueの場合はNot_finaleを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 8,
                mediaType = "tv",
                numEpisodes = null,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(10, media.id, hasNextEpisode = true)

            // Then
            assertEquals(FinaleState.NOT_FINALE, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("numEpisodesがnullでhasNextEpisode=falseでfinished_airingの場合はunknownを返す")
        fun numEpisodesがnullでhasNextEpisodeFalseでfinishedAiringの場合はUnknownを返す() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 9,
                mediaType = "tv",
                numEpisodes = null,
                status = "finished_airing",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(10, media.id, hasNextEpisode = false)

            // Then
            assertEquals(FinaleState.UNKNOWN, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("上記いずれの条件も満たさない場合はunknownを返す")
        fun otherwise() = runTest {
            // Given
            val media = MyAnimeListResponse(
                id = 10,
                mediaType = "tv",
                numEpisodes = 12,
                status = "not_yet_aired",
                broadcast = null,
                mainPicture = null
            )
            coEvery { myAnimeListRepository.getAnimeDetail(media.id) } returns Result.success(media)

            // When
            val result = judgeFinaleUseCase(10, media.id)

            // Then
            assertEquals(FinaleState.UNKNOWN, result.state)
            assertFalse(result.isFinale)
        }

        @Test
        @DisplayName("MyAnimeList取得失敗時はunknownを返す")
        fun MyAnimeList取得失敗時はunknownを返す() = runTest {
            // Given
            coEvery { myAnimeListRepository.getAnimeDetail(any()) } returns Result.failure(Exception("API Error"))

            // When
            val result = judgeFinaleUseCase(10, 123)

            // Then
            assertEquals(FinaleState.UNKNOWN, result.state)
            assertFalse(result.isFinale)
        }
    }
}
