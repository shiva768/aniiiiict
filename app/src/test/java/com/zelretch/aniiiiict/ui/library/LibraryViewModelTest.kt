package com.zelretch.aniiiiict.ui.library

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
@DisplayName("LibraryViewModel")
class LibraryViewModelTest {

    private lateinit var loadLibraryEntriesUseCase: LoadLibraryEntriesUseCase
    private lateinit var loadProgramsUseCase: LoadProgramsUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        loadLibraryEntriesUseCase = mockk()
        loadProgramsUseCase = mockk()
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(emptyList())

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returnsMany listOf(
                Result.success(initialEntries),
                Result.success(refreshedEntries)
            )

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(emptyList())

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
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
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.failure(exception)
            every { errorMapper.toUserMessage(exception) } returns "ネットワークエラーが発生しました"

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals("ネットワークエラーが発生しました", state.error)
            assertFalse(state.isLoading)
            assertEquals(emptyList<LibraryEntry>(), state.entries)
        }
    }

    @Nested
    @DisplayName("メディアフィルター")
    inner class MediaFilter {

        @Test
        @DisplayName("availableMediaがエントリーから抽出される")
        fun availableMediaがエントリーから抽出される() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Work 1", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Work 2", media = "MOVIE"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry3",
                    work = createFakeWork("work3", "Work 3", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(listOf("MOVIE", "TV"), state.availableMedia)
        }

        @Test
        @DisplayName("toggleMediaFilterで選択が追加される")
        fun toggleMediaFilterで選択が追加される() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Work 1", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.toggleMediaFilter("TV")
            val state = viewModel.uiState.first()

            // Then
            assertTrue("TV" in state.filterState.selectedMedia)
        }

        @Test
        @DisplayName("toggleMediaFilterで選択が解除される")
        fun toggleMediaFilterで選択が解除される() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Work 1", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            viewModel.uiState.first { !it.isLoading }
            viewModel.toggleMediaFilter("TV")
            viewModel.uiState.first { "TV" in it.filterState.selectedMedia }

            // When
            viewModel.toggleMediaFilter("TV")
            val state = viewModel.uiState.first()

            // Then
            assertFalse("TV" in state.filterState.selectedMedia)
        }

        @Test
        @DisplayName("メディアフィルターが適用されエントリーが絞り込まれる")
        fun メディアフィルターが適用されエントリーが絞り込まれる() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "TV Work", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Movie Work", media = "MOVIE"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.toggleMediaFilter("TV")
            val state = viewModel.uiState.first()

            // Then
            assertEquals(1, state.entries.size)
            assertEquals("entry1", state.entries[0].id)
        }

        @Test
        @DisplayName("メディアフィルターが空の場合は全エントリーが表示される")
        fun メディアフィルターが空の場合は全エントリーが表示される() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "TV Work", media = "TV"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Movie Work", media = "MOVIE"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadProgramsUseCase() } returns Result.success(emptyList())
            coEvery { loadLibraryEntriesUseCase(listOf(StatusState.WATCHING)) } returns Result.success(fakeEntries)

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, loadProgramsUseCase, errorMapper)
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(2, state.entries.size)
            assertTrue(state.filterState.selectedMedia.isEmpty())
        }
    }

    private fun createFakeWork(id: String, title: String, media: String? = null) = Work(
        id = id,
        title = title,
        seasonName = null,
        seasonYear = null,
        media = media,
        malAnimeId = null,
        viewerStatusState = StatusState.WATCHING,
        image = null
    )
}
