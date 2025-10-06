package com.zelretch.aniiiiict.ui.track

import android.content.Context
import app.cash.turbine.test
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.filter.AvailableFilters
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TrackViewModel")
class TrackViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val filterStateFlow = MutableStateFlow(FilterState())
    private lateinit var filterPreferences: FilterPreferences
    private lateinit var loadProgramsUseCase: LoadProgramsUseCase
    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var filterProgramsUseCase: FilterProgramsUseCase
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var errorMapper: ErrorMapper
    private lateinit var viewModel: TrackViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)

        filterPreferences = mockk {
            every { filterState } returns filterStateFlow
        }
        loadProgramsUseCase = mockk()
        watchEpisodeUseCase = mockk()
        mockk<BulkRecordEpisodesUseCase>()
        updateViewStateUseCase = mockk()
        filterProgramsUseCase = mockk()
        judgeFinaleUseCase = mockk()
        errorMapper = mockk(relaxed = true)
        mockk<Context>(relaxed = true)

        // デフォルトで空リストを返すflow
        coEvery { loadProgramsUseCase.invoke() } returns flowOf(emptyList())
        every { filterProgramsUseCase.invoke(any(), any()) } answers { firstArg() }
        every { filterProgramsUseCase.extractAvailableFilters(any()) } returns AvailableFilters(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

        viewModel = TrackViewModel(
            loadProgramsUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase,
            filterProgramsUseCase,
            filterPreferences,
            judgeFinaleUseCase,
            errorMapper
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("プログラム一覧ロード")
    inner class LoadPrograms {

        @Test
        @DisplayName("正常にロードできる場合UIStateにプログラムがセットされる")
        fun onSuccess() = runTest(dispatcher) {
            // Given
            val fakePrograms = listOf<ProgramWithWork>(mockk(relaxed = true))
            coEvery { loadProgramsUseCase.invoke() } returns flowOf(fakePrograms)
            every { filterProgramsUseCase.invoke(any(), any()) } returns fakePrograms
            every { filterProgramsUseCase.extractAvailableFilters(any()) } returns AvailableFilters(
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
            )

            // When
            filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy"))
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val updated = viewModel.uiState.value
            assertEquals(fakePrograms, updated.programs)
            assertFalse(updated.isLoading)
            assertNull(updated.error)
        }

        @Test
        @DisplayName("例外が発生する場合UIStateにエラーがセットされる")
        fun onException() = runTest(dispatcher) {
            // Given
            every { errorMapper.toUserMessage(any(), any()) } returns "処理中にエラーが発生しました"
            coEvery { loadProgramsUseCase.invoke() } returns flow {
                throw LoadProgramsException("error")
            }

            // When
            filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy-error"))
            dispatcher.scheduler.advanceUntilIdle()

            // Then
            val errorState = viewModel.uiState.value
            assertFalse(errorState.isLoading)
            assertEquals("処理中にエラーが発生しました", errorState.error)
        }
    }

    @Nested
    @DisplayName("フィナーレ判定ロジック")
    inner class FinaleJudgment {

        @Test
        @DisplayName("最終話を記録し確認ダイアログでいいえを選択するとステータスは更新されずダイアログが閉じる")
        fun finaleDialogNo() = runTest {
            // Given
            val workId = "work-finale-no"
            val episodeId = "ep-final-12-no"
            val fakePrograms = createTestProgram(workId, episodeId)
            coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
            coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                com.zelretch.aniiiiict.domain.usecase.FinaleState.FINALE_CONFIRMED
            )

            // When & Then
            viewModel.uiState.test {
                skipItems(1)
                viewModel.refresh()

                viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                skipItems(1)
                val finaleConfirmationState = awaitItem()
                assertEquals(workId, finaleConfirmationState.showFinaleConfirmationForWorkId)

                viewModel.dismissFinaleConfirmation()

                val finalState = awaitItem()
                assertNull(finalState.showFinaleConfirmationForWorkId)

                coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
            }
        }

        @Test
        @DisplayName("フィナーレ判定に失敗した場合フィナーレ確認は表示されずステータス更新もされない")
        fun finaleJudgmentFailure() = runTest {
            // Given
            val workId = "work-unk-ep"
            val episodeId = "ep5"
            val fakePrograms = createTestProgram(workId, episodeId)

            coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
            coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                com.zelretch.aniiiiict.domain.usecase.FinaleState.UNKNOWN
            )

            // When & Then
            viewModel.uiState.test {
                skipItems(1)
                viewModel.refresh()
                viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                skipItems(1)
                awaitItem() // isLoading=true
                val finalState = awaitItem() // isLoading=false
                assertFalse(finalState.isLoading)
                assertNull(finalState.showFinaleConfirmationForWorkId)

                coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
            }
        }

        @Test
        @DisplayName("フィナーレ判定UNKNOWNの場合ステータス更新されない")
        fun finaleUnknown() = runTest {
            // Given
            val workId = "work1"
            val episodeId = "ep1"
            val fakePrograms = createTestProgram(workId, episodeId)

            coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
            coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                com.zelretch.aniiiiict.domain.usecase.FinaleState.UNKNOWN
            )

            // When & Then
            viewModel.uiState.test {
                skipItems(1)
                viewModel.refresh()

                viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                skipItems(1)
                awaitItem() // isLoading=true
                val finalState = awaitItem() // isLoading=false
                assertFalse(finalState.isLoading)
                assertNull(finalState.showFinaleConfirmationForWorkId)

                coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
            }
        }
    }
}

private fun createTestProgram(workId: String, episodeId: String): ProgramWithWork {
    val work = Work(
        id = workId,
        title = "Test Anime",
        seasonName = SeasonName.SPRING,
        seasonYear = 2024,
        media = "TV",
        mediaText = "TV",
        viewerStatusState = StatusState.WATCHING,
        malAnimeId = "123"
    )
    val episode = Episode(
        id = episodeId,
        title = "Episode Title",
        numberText = "1",
        number = 1
    )
    val program = Program(
        id = "prog1",
        startedAt = LocalDateTime.now(),
        channel = Channel(name = "Channel"),
        episode = episode
    )
    return ProgramWithWork(
        programs = listOf(program),
        firstProgram = program,
        work = work
    )
}

private class LoadProgramsException(message: String) : RuntimeException(message)
