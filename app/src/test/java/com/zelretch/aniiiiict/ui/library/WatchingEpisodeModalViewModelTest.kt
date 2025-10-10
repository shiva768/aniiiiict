package com.zelretch.aniiiiict.ui.library

import app.cash.turbine.test
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.error.DomainError
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
@DisplayName("WatchingEpisodeModalViewModel")
class WatchingEpisodeModalViewModelTest {

    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: WatchingEpisodeModalViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        watchEpisodeUseCase = mockk()
        updateViewStateUseCase = mockk()
        errorMapper = mockk {
            every { toUserMessage(any(), any()) } returns "エラーが発生しました"
        }
        viewModel = WatchingEpisodeModalViewModel(
            watchEpisodeUseCase,
            updateViewStateUseCase,
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
        @DisplayName("LibraryEntryを渡すとstateが正しく更新される")
        fun LibraryEntryを渡すとstateが正しく更新される() = runTest(dispatcher) {
            // Given
            val episode = mockk<Episode> {
                every { id } returns "ep-1"
                every { number } returns 1
            }
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns episode
                every { this@mockk.work } returns work
            }

            // When & Then
            viewModel.state.test {
                val initial = awaitItem()
                assertNull(initial.episode)

                viewModel.initialize(entry)
                val updated = awaitItem()
                assertEquals(episode, updated.episode)
                assertEquals("work-id", updated.workId)
                assertEquals("テスト作品", updated.workTitle)
                assertEquals(StatusState.WATCHING, updated.selectedStatus)
            }
        }

        @Test
        @DisplayName("nextEpisodeがnullの場合でも初期化できる")
        fun nextEpisodeがnullの場合でも初期化できる() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns null
                every { this@mockk.work } returns work
            }

            // When & Then
            viewModel.state.test {
                awaitItem()
                viewModel.initialize(entry)
                val updated = awaitItem()
                assertNull(updated.episode)
                assertEquals("work-id", updated.workId)
                assertEquals("テスト作品", updated.workTitle)
            }
        }
    }

    @Nested
    @DisplayName("ステータス変更")
    inner class ChangeStatus {

        @Test
        @DisplayName("ステータス変更が成功するとイベントが発行される")
        fun ステータス変更が成功するとイベントが発行される() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns null
                every { this@mockk.work } returns work
            }
            viewModel.initialize(entry)

            coEvery { updateViewStateUseCase("work-id", StatusState.WATCHED) } returns Result.success(Unit)

            // When & Then
            viewModel.events.test {
                viewModel.changeStatus(StatusState.WATCHED)
                val event = awaitItem()
                assertTrue(event is WatchingEpisodeModalEvent.StatusChanged)
            }

            // State should be updated
            val state = viewModel.state.value
            assertEquals(StatusState.WATCHED, state.selectedStatus)
            assertFalse(state.isStatusChanging)
            assertNull(state.statusChangeError)
        }

        @Test
        @DisplayName("ステータス変更が失敗すると元のステータスに戻る")
        fun ステータス変更が失敗すると元のステータスに戻る() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns null
                every { this@mockk.work } returns work
            }
            viewModel.initialize(entry)

            coEvery {
                updateViewStateUseCase("work-id", StatusState.WATCHED)
            } returns Result.failure(DomainError.NetworkError.Unknown(cause = Exception("Network error")))

            // When
            viewModel.changeStatus(StatusState.WATCHED)

            // Then
            val state = viewModel.state.value
            assertEquals(StatusState.WATCHING, state.selectedStatus) // Roll back
            assertEquals("エラーが発生しました", state.statusChangeError)
            assertFalse(state.isStatusChanging)
        }
    }

    @Nested
    @DisplayName("エピソード記録")
    inner class RecordEpisode {

        @Test
        @DisplayName("エピソード記録が成功するとイベントが発行される")
        fun エピソード記録が成功するとイベントが発行される() = runTest(dispatcher) {
            // Given
            val episode = mockk<Episode> {
                every { id } returns "ep-1"
                every { number } returns 1
            }
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns episode
                every { this@mockk.work } returns work
            }
            viewModel.initialize(entry)

            coEvery {
                watchEpisodeUseCase("ep-1", "work-id", StatusState.WATCHING)
            } returns Result.success(Unit)

            // When & Then
            viewModel.events.test {
                viewModel.recordEpisode()
                val event = awaitItem()
                assertTrue(event is WatchingEpisodeModalEvent.EpisodeRecorded)
            }
        }

        @Test
        @DisplayName("エピソード記録が失敗するとエラーメッセージが表示される")
        fun エピソード記録が失敗するとエラーメッセージが表示される() = runTest(dispatcher) {
            // Given
            val episode = mockk<Episode> {
                every { id } returns "ep-1"
                every { number } returns 1
            }
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns episode
                every { this@mockk.work } returns work
            }
            viewModel.initialize(entry)

            coEvery {
                watchEpisodeUseCase("ep-1", "work-id", StatusState.WATCHING)
            } returns Result.failure(DomainError.NetworkError.Unknown(cause = Exception("Network error")))

            // When
            viewModel.recordEpisode()

            // Then
            val state = viewModel.state.value
            assertEquals("エラーが発生しました", state.statusChangeError)
        }

        @Test
        @DisplayName("エピソードがnullの場合は何もしない")
        fun エピソードがnullの場合は何もしない() = runTest(dispatcher) {
            // Given
            val work = mockk<Work> {
                every { id } returns "work-id"
                every { title } returns "テスト作品"
                every { viewerStatusState } returns StatusState.WATCHING
            }
            val entry = mockk<LibraryEntry> {
                every { nextEpisode } returns null
                every { this@mockk.work } returns work
            }
            viewModel.initialize(entry)

            // When
            viewModel.recordEpisode()

            // Then - no crash, no event
            val state = viewModel.state.value
            assertNull(state.episode)
        }
    }
}
