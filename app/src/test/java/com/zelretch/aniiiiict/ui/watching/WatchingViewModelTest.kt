package com.zelretch.aniiiiict.ui.watching

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
@DisplayName("WatchingViewModel")
class WatchingViewModelTest {

    private lateinit var loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        loadLibraryEntriesUseCase = mockk()
        errorMapper = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初期化")
    inner class Initialization {

        @Test
        @DisplayName("loadLibraryEntriesが呼ばれUIステートが初期値で更新される")
        fun loadLibraryEntriesが呼ばれUIステートが初期値で更新される() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns flowOf(emptyList())

            // When
            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(emptyList<LibraryEntry>(), state.entries)
            assertEquals(emptyList<LibraryEntry>(), state.allEntries)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertTrue(state.showOnlyPastWorks)
        }

        @Test
        @DisplayName("エントリーが存在する場合正しく読み込まれる")
        fun withEntries() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Test Work 1"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Test Work 2"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns flowOf(fakeEntries)

            // When
            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(2, state.entries.size)
            assertEquals(2, state.allEntries.size)
            assertEquals("entry1", state.entries[0].id)
            assertEquals("entry2", state.entries[1].id)
            assertFalse(state.isLoading)
        }
    }

    @Nested
    @DisplayName("リフレッシュ")
    inner class Refresh {

        @Test
        @DisplayName("refreshが呼ばれた時再読み込みが実行される")
        fun refreshReloadsEntries() = runTest(dispatcher) {
            // Given
            val initialEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Initial Work"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            val refreshedEntries = listOf(
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Refreshed Work"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returnsMany listOf(
                flowOf(initialEntries),
                flowOf(refreshedEntries)
            )

            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.refresh()
            val state = viewModel.uiState.first { !it.isLoading && it.entries.isNotEmpty() }

            // Then
            assertEquals(1, state.entries.size)
            assertEquals("entry2", state.entries[0].id)
            assertEquals("Refreshed Work", state.entries[0].work.title)
        }
    }

    @Nested
    @DisplayName("フィルター切り替え")
    inner class ToggleFilter {

        @Test
        @DisplayName("過去作のみフィルターが切り替わる")
        fun togglePastWorksFilter() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Test Work"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns flowOf(fakeEntries)

            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            val initialState = viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.togglePastWorksFilter()
            val toggledState = viewModel.uiState.first()

            // Then
            assertTrue(initialState.showOnlyPastWorks)
            assertFalse(toggledState.showOnlyPastWorks)
        }

        @Test
        @DisplayName("フィルター表示が切り替わる")
        fun toggleFilterVisibility() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns flowOf(emptyList())

            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            val initialState = viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.toggleFilterVisibility()
            val toggledState = viewModel.uiState.first()

            // Then
            assertFalse(initialState.isFilterVisible)
            assertTrue(toggledState.isFilterVisible)
        }
    }

    @Nested
    @DisplayName("エラーハンドリング")
    inner class ErrorHandling {

        @Test
        @DisplayName("エラー発生時エラーメッセージが表示される")
        fun showsErrorMessage() = runTest(dispatcher) {
            // Given
            val exception = RuntimeException("Network error")
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns kotlinx.coroutines.flow.flow {
                throw exception
            }
            every { errorMapper.toUserMessage(exception) } returns "ネットワークエラーが発生しました"

            // When
            val viewModel = WatchingViewModel(loadLibraryEntriesUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals("ネットワークエラーが発生しました", state.error)
            assertFalse(state.isLoading)
            assertEquals(emptyList<LibraryEntry>(), state.entries)
        }
    }

    private fun createFakeWork(id: String, title: String) = Work(
        id = id,
        title = title,
        seasonName = null,
        seasonYear = null,
        media = null,
        malAnimeId = null,
        viewerStatusState = StatusState.WATCHING,
        image = null
    )
}
