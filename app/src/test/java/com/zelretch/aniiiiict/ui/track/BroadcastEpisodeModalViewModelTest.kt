package com.zelretch.aniiiiict.ui.track

import app.cash.turbine.test
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BroadcastEpisodeModalViewModel")
class BroadcastEpisodeModalViewModelTest {

    private lateinit var bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase
    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: BroadcastEpisodeModalViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        bulkRecordEpisodesUseCase = mockk()
        watchEpisodeUseCase = mockk()
        updateViewStateUseCase = mockk()
        judgeFinaleUseCase = mockk()
        errorMapper = mockk(relaxed = true)
        viewModel = BroadcastEpisodeModalViewModel(
            bulkRecordEpisodesUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase,
            judgeFinaleUseCase,
            errorMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初期化")
    inner class Initialize {

        @Test
        @DisplayName("ProgramWithWorkを渡すとstateが正しく更新される")
        fun ProgramWithWorkを渡すとstateが正しく更新される() = runTest(dispatcher) {
            // Given
            val program = mockk<Program>()
            val work = mockk<Work> {
                every { viewerStatusState } returns StatusState.WATCHING
                every { id } returns "work-id"
                every { malAnimeId } returns "123"
            }
            val programWithWork = ProgramWithWork(
                programs = listOf(program),
                firstProgram = program,
                work = work
            )

            // When & Then
            viewModel.state.test {
                awaitItem()
                viewModel.initialize(programWithWork)
                val updated = awaitItem()
                assertEquals(listOf(program), updated.programs)
                assertEquals(StatusState.WATCHING, updated.selectedStatus)
                assertEquals("work-id", updated.workId)
                assertEquals("123", updated.malAnimeId)
            }
        }
    }

    @Nested
    @DisplayName("確認ダイアログ表示")
    inner class ShowConfirmDialog {

        @Test
        @DisplayName("indexを渡すとダイアログが表示状態になる")
        fun indexを渡すとダイアログが表示状態になる() = runTest(dispatcher) {
            // When & Then
            viewModel.state.test {
                awaitItem() // 初期値
                viewModel.showConfirmDialog(2)
                val updated = awaitItem()
                assertTrue(updated.showConfirmDialog)
                assertEquals(2, updated.selectedEpisodeIndex)
            }
        }
    }

    @Nested
    @DisplayName("単一エピソード記録でのフィナーレ判定")
    inner class SingleEpisodeFinaleJudgment {

        @Test
        @DisplayName("最終話を記録した場合フィナーレ確認ダイアログが表示される")
        fun onFinaleEpisode() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { viewerStatusState } returns StatusState.WATCHING
                every { id } returns "work-finale"
                every { malAnimeId } returns "123"
            }
            val episode = mockk<com.zelretch.aniiiiict.data.model.Episode> {
                every { id } returns "ep-12"
                every { number } returns 12
                every { title } returns "最終話"
                every { hasNextEpisode } returns false
            }
            val program = mockk<Program> {
                every { this@mockk.episode } returns episode
                every { id } returns "prog-12"
            }
            val programWithWork = ProgramWithWork(
                programs = listOf(program),
                firstProgram = program,
                work = work
            )

            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery {
                judgeFinaleUseCase(12, 123, false)
            } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)

            viewModel.initialize(programWithWork)

            // When
            viewModel.recordEpisode("ep-12", StatusState.WATCHING)

            // Then
            val state = viewModel.state.value
            assertTrue(state.showSingleEpisodeFinaleConfirmation)
            assertEquals(12, state.singleEpisodeFinaleNumber)
            assertEquals("work-finale", state.singleEpisodeFinaleWorkId)
        }

        @Test
        @DisplayName("最終話ではないエピソードを記録した場合フィナーレ確認ダイアログは表示されない")
        fun onRegularEpisode() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { viewerStatusState } returns StatusState.WATCHING
                every { id } returns "work-normal"
                every { malAnimeId } returns "123"
            }
            val episode = mockk<com.zelretch.aniiiiict.data.model.Episode> {
                every { id } returns "ep-10"
                every { number } returns 10
                every { title } returns "第10話"
                every { hasNextEpisode } returns true
            }
            val program = mockk<Program> {
                every { this@mockk.episode } returns episode
                every { id } returns "prog-10"
            }
            val programWithWork = ProgramWithWork(
                programs = listOf(program),
                firstProgram = program,
                work = work
            )

            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery {
                judgeFinaleUseCase(10, 123, true)
            } returns JudgeFinaleResult(FinaleState.NOT_FINALE)

            viewModel.initialize(programWithWork)

            // When
            viewModel.recordEpisode("ep-10", StatusState.WATCHING)

            // Then
            val state = viewModel.state.value
            assertFalse(state.showSingleEpisodeFinaleConfirmation)
            assertNull(state.singleEpisodeFinaleNumber)
            assertNull(state.singleEpisodeFinaleWorkId)
        }
    }
}
