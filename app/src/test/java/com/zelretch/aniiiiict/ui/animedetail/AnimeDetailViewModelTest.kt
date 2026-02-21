package com.zelretch.aniiiiict.ui.animedetail

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import com.zelretch.aniiiiict.ui.base.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AnimeDetailViewModel")
class AnimeDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getAnimeDetailUseCase: GetAnimeDetailUseCase
    private lateinit var errorMapper: ErrorMapper
    private lateinit var viewModel: AnimeDetailViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAnimeDetailUseCase = mockk()
        errorMapper = mockk(relaxed = true)
        viewModel = AnimeDetailViewModel(getAnimeDetailUseCase, errorMapper)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初期状態")
    inner class InitialState {

        @Test
        @DisplayName("ローディング状態で初期化される")
        fun initialLoading() {
            // When
            val initialState = viewModel.uiState.value

            // Then
            assertTrue(initialState is UiState.Loading)
        }
    }

    @Nested
    @DisplayName("アニメ詳細の読み込み")
    inner class LoadAnimeDetail {

        @Test
        @DisplayName("成功時にUIStateが更新される")
        fun onSuccess() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val animeDetailInfo = createSampleAnimeDetailInfo()
            every { errorMapper.toUserMessage(any(), any()) } returns "エラーメッセージ"

            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(animeDetailInfo)

            // When
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertNotNull((state as UiState.Success).data.animeDetailInfo)
            assertEquals("テストアニメ", state.data.animeDetailInfo.work.title)
        }

        @Test
        @DisplayName("失敗時にエラーメッセージが表示される")
        fun onError() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            every { errorMapper.toUserMessage(any(), any()) } returns "エラーが発生しました"

            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.failure(Exception("API Error"))

            // When
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Error)
            assertEquals("エラーが発生しました", (state as UiState.Error).message)
        }
    }

    private fun createSampleProgramWithWork(): ProgramWithWork {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        val episode = Episode(
            id = "episode-id",
            number = 1,
            numberText = "1",
            title = "第1話"
        )

        val channel = Channel(name = "テストチャンネル")

        val program = Program(
            id = "program-id",
            startedAt = LocalDateTime.now(),
            channel = channel,
            episode = episode
        )

        return ProgramWithWork(
            work = work,
            programs = listOf(program)
        )
    }

    private fun createSampleAnimeDetailInfo(): AnimeDetailInfo {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        val malInfo = MyAnimeListResponse(
            id = 12345,
            mediaType = "tv",
            numEpisodes = 24,
            status = "currently_airing",
            broadcast = null,
            mainPicture = null
        )

        return AnimeDetailInfo(
            work = work,
            programs = null,
            seriesList = null,
            malInfo = malInfo,
            episodeCount = 24,
            imageUrl = "https://example.com/image.jpg",
            officialSiteUrl = "https://example.com/official",
            wikipediaUrl = "https://ja.wikipedia.org/wiki/テストアニメ"
        )
    }
}
