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
import com.zelretch.aniiiiict.data.model.WorkPriority
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.domain.usecase.GetWorkPriorityUseCase
import com.zelretch.aniiiiict.domain.usecase.SetWorkPriorityUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import com.zelretch.aniiiiict.ui.base.UiState
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var getWorkPriorityUseCase: GetWorkPriorityUseCase
    private lateinit var setWorkPriorityUseCase: SetWorkPriorityUseCase
    private lateinit var errorMapper: ErrorMapper
    private lateinit var viewModel: AnimeDetailViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAnimeDetailUseCase = mockk()
        updateViewStateUseCase = mockk()
        getWorkPriorityUseCase = mockk()
        setWorkPriorityUseCase = mockk()
        errorMapper = mockk(relaxed = true)
        coEvery { getWorkPriorityUseCase(any()) } returns WorkPriority.NONE
        viewModel = AnimeDetailViewModel(
            getAnimeDetailUseCase,
            updateViewStateUseCase,
            getWorkPriorityUseCase,
            setWorkPriorityUseCase,
            errorMapper
        )
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
            assertEquals(StatusState.WATCHING, state.data.selectedStatus)
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

    @Nested
    @DisplayName("workIdのみでのアニメ詳細の読み込み")
    inner class LoadAnimeDetailById {

        @Test
        @DisplayName("成功時にUIStateが更新される")
        fun onSuccess() = runTest {
            // Given
            val workId = "test-work-id"
            val animeDetailInfo = createSampleAnimeDetailInfo()
            every { errorMapper.toUserMessage(any(), any()) } returns "エラーメッセージ"

            coEvery { getAnimeDetailUseCase(workId) } returns Result.success(animeDetailInfo)

            // When
            viewModel.loadAnimeDetailById(workId)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals("テストアニメ", (state as UiState.Success).data.animeDetailInfo.work.title)
            assertEquals(StatusState.WATCHING, state.data.selectedStatus)
        }

        @Test
        @DisplayName("失敗時にエラーメッセージが表示される")
        fun onError() = runTest {
            // Given
            val workId = "test-work-id"
            every { errorMapper.toUserMessage(any(), any()) } returns "エラーが発生しました"

            coEvery { getAnimeDetailUseCase(workId) } returns Result.failure(Exception("API Error"))

            // When
            viewModel.loadAnimeDetailById(workId)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Error)
            assertEquals("エラーが発生しました", (state as UiState.Error).message)
        }
    }

    @Nested
    @DisplayName("ステータス変更")
    inner class ChangeStatus {

        @Test
        @DisplayName("成功時にステータスが更新される")
        fun onSuccess() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val animeDetailInfo = createSampleAnimeDetailInfo()
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(animeDetailInfo)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.success(Unit)
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // When
            viewModel.changeStatus(StatusState.WATCHED)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(StatusState.WATCHED, (state as UiState.Success).data.selectedStatus)
            // 遷移元へ反映するためのフラグが立つ
            assertTrue(state.data.statusChanged)
        }

        @Test
        @DisplayName("失敗時にステータスが元に戻りエラーメッセージが表示される")
        fun onFailure() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val animeDetailInfo = createSampleAnimeDetailInfo()
            every { errorMapper.toUserMessage(any(), any()) } returns "ステータス変更に失敗しました"
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(animeDetailInfo)
            coEvery { updateViewStateUseCase(any(), any()) } returns Result.failure(Exception("API Error"))
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // When
            viewModel.changeStatus(StatusState.WATCHED)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(StatusState.WATCHING, (state as UiState.Success).data.selectedStatus)
            assertEquals("ステータス変更に失敗しました", state.data.statusChangeError)
            // 失敗時はフラグを立てない（遷移元を無駄にリフレッシュしない）
            assertEquals(false, state.data.statusChanged)
        }
    }

    @Nested
    @DisplayName("優先度の読み込み")
    inner class LoadPriority {

        @Test
        @DisplayName("詳細取得成功時に端末内の優先度が反映される")
        fun loadWithProgramWithWork() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(createSampleAnimeDetailInfo())
            coEvery { getWorkPriorityUseCase("test-work-id") } returns WorkPriority.TIER1

            // When
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(WorkPriority.TIER1, (state as UiState.Success).data.priority)
            assertEquals(false, state.data.priorityChanged)
        }

        @Test
        @DisplayName("workIdのみでの詳細取得成功時にも優先度が反映される")
        fun loadById() = runTest {
            // Given
            val workId = "test-work-id"
            coEvery { getAnimeDetailUseCase(workId) } returns Result.success(createSampleAnimeDetailInfo())
            coEvery { getWorkPriorityUseCase(workId) } returns WorkPriority.TIER3

            // When
            viewModel.loadAnimeDetailById(workId)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(WorkPriority.TIER3, (state as UiState.Success).data.priority)
        }
    }

    @Nested
    @DisplayName("優先度変更")
    inner class ChangePriority {

        @Test
        @DisplayName("成功時に優先度が更新され変更フラグが立つ")
        fun onSuccess() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(createSampleAnimeDetailInfo())
            coEvery { setWorkPriorityUseCase(any(), any()) } returns Result.success(Unit)
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // When
            viewModel.changePriority(WorkPriority.TIER1)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(WorkPriority.TIER1, (state as UiState.Success).data.priority)
            assertTrue(state.data.priorityChanged)
            // ステータス変更フラグには影響しない
            assertEquals(false, state.data.statusChanged)
            coVerify(exactly = 1) { setWorkPriorityUseCase("test-work-id", WorkPriority.TIER1) }
        }

        @Test
        @DisplayName("失敗時に優先度が元に戻りエラーメッセージが表示される")
        fun onFailure() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            every { errorMapper.toUserMessage(any(), any()) } returns "優先度の変更に失敗しました"
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(createSampleAnimeDetailInfo())
            coEvery { setWorkPriorityUseCase(any(), any()) } returns Result.failure(Exception("DB Error"))
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // When
            viewModel.changePriority(WorkPriority.TIER3)
            testScheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is UiState.Success)
            assertEquals(WorkPriority.NONE, (state as UiState.Success).data.priority)
            assertEquals("優先度の変更に失敗しました", state.data.statusChangeError)
            assertEquals(false, state.data.priorityChanged)
        }

        @Test
        @DisplayName("同じ優先度を選んだ場合は保存しない")
        fun samePriority() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            coEvery { getAnimeDetailUseCase(programWithWork) } returns Result.success(createSampleAnimeDetailInfo())
            viewModel.loadAnimeDetail(programWithWork)
            testScheduler.advanceUntilIdle()

            // When
            viewModel.changePriority(WorkPriority.NONE)
            testScheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { setWorkPriorityUseCase(any(), any()) }
            val state = viewModel.uiState.value as UiState.Success
            assertEquals(false, state.data.priorityChanged)
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
