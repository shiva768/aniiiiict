package com.zelretch.aniiiiict.ui.library

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.sync.LibrarySyncService
import com.zelretch.aniiiiict.domain.sync.SyncStatus
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var librarySyncService: LibrarySyncService
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        loadLibraryEntriesUseCase = mockk()
        librarySyncService = mockk()
        errorMapper = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LibraryViewModel {
        every { librarySyncService.status } returns MutableStateFlow(SyncStatus.Idle)
        return LibraryViewModel(loadLibraryEntriesUseCase, librarySyncService, errorMapper)
    }

    @Nested
    @DisplayName("初期化")
    inner class Initialization {

        @Test
        @DisplayName("Roomからエントリーを読み込みUIステートが更新される")
        fun loadsEntriesFromRoom() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(emptyList())

            // When
            val viewModel = createViewModel()
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(emptyList<LibraryEntry>(), state.entries)
            assertFalse(state.isLoading)
            assertNull(state.error)
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
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            // When
            val viewModel = createViewModel()
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
    @DisplayName("同期状態")
    inner class SyncState {

        @Test
        @DisplayName("同期中はisSyncingがtrueになる")
        fun syncingStateShowsIsSyncing() = runTest(dispatcher) {
            // Given
            val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Syncing)
            every { librarySyncService.status } returns syncStatusFlow
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(emptyList())

            // When
            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, librarySyncService, errorMapper)
            val state = viewModel.uiState.first { it.isSyncing }

            // Then
            assertTrue(state.isSyncing)
        }

        @Test
        @DisplayName("Idle遷移時にRoomを再読み込みする")
        fun idleTransitionReloadsFromRoom() = runTest(dispatcher) {
            // Given
            val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Syncing)
            every { librarySyncService.status } returns syncStatusFlow
            val entries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Loaded Work"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(entries)

            val viewModel = LibraryViewModel(loadLibraryEntriesUseCase, librarySyncService, errorMapper)
            viewModel.uiState.first { it.isSyncing }

            // When
            syncStatusFlow.value = SyncStatus.Idle
            val state = viewModel.uiState.first { !it.isLoading && it.entries.isNotEmpty() }

            // Then
            assertEquals(1, state.entries.size)
            assertEquals("entry1", state.entries[0].id)
        }
    }

    @Nested
    @DisplayName("フィルター切り替え")
    inner class ToggleFilter {

        @Test
        @DisplayName("フィルター表示が切り替わる")
        fun toggleFilterVisibility() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(emptyList())

            val viewModel = createViewModel()
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
            val exception = RuntimeException("DB error")
            coEvery { loadLibraryEntriesUseCase() } returns Result.failure(exception)
            every { errorMapper.toUserMessage(exception) } returns "読み込みエラーが発生しました"

            // When
            val viewModel = createViewModel()
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals("読み込みエラーが発生しました", state.error)
            assertFalse(state.isLoading)
            assertEquals(emptyList<LibraryEntry>(), state.entries)
        }
    }

    @Nested
    @DisplayName("検索フィルター")
    inner class SearchFilter {

        @Test
        @DisplayName("タイトル検索でエントリーが絞り込まれる")
        fun titleSearchFiltersEntries() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "天国大魔境"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "進撃の巨人"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.updateSearchQuery("天国")
            val state = viewModel.uiState.first()

            // Then
            assertEquals(1, state.entries.size)
            assertEquals("entry1", state.entries[0].id)
        }

        @Test
        @DisplayName("検索クエリが空の場合は全エントリーが表示される")
        fun emptyQueryShowsAllEntries() = runTest(dispatcher) {
            // Given
            val fakeEntries = listOf(
                LibraryEntry(
                    id = "entry1",
                    work = createFakeWork("work1", "Work 1"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                ),
                LibraryEntry(
                    id = "entry2",
                    work = createFakeWork("work2", "Work 2"),
                    nextEpisode = null,
                    statusState = StatusState.WATCHING
                )
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.updateSearchQuery("")
            val state = viewModel.uiState.first()

            // Then
            assertEquals(2, state.entries.size)
        }
    }

    @Nested
    @DisplayName("メディアフィルター")
    inner class MediaFilter {

        @Test
        @DisplayName("availableMediaがエントリーから抽出される")
        fun availableMediaExtractedFromEntries() = runTest(dispatcher) {
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
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            // When
            val viewModel = createViewModel()
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(listOf("MOVIE", "TV"), state.availableMedia)
        }

        @Test
        @DisplayName("メディアフィルターが適用されエントリーが絞り込まれる")
        fun mediaFilterFiltersEntries() = runTest(dispatcher) {
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
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            val viewModel = createViewModel()
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
        fun emptyMediaFilterShowsAllEntries() = runTest(dispatcher) {
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
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(fakeEntries)

            // When
            val viewModel = createViewModel()
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(2, state.entries.size)
            assertTrue(state.filterState.selectedMedia.isEmpty())
        }

        @Test
        @DisplayName("toggleMediaFilterで選択が解除される")
        fun toggleMediaFilterDeselects() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(
                listOf(
                    LibraryEntry(
                        id = "entry1",
                        work = createFakeWork("work1", "Work 1", media = "TV"),
                        nextEpisode = null,
                        statusState = StatusState.WATCHING
                    )
                )
            )

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }
            viewModel.toggleMediaFilter("TV")
            viewModel.uiState.first { "TV" in it.filterState.selectedMedia }

            // When
            viewModel.toggleMediaFilter("TV")
            val state = viewModel.uiState.first()

            // Then
            assertFalse("TV" in state.filterState.selectedMedia)
        }
    }

    @Nested
    @DisplayName("エントリー更新")
    inner class EntryUpdate {

        @Test
        @DisplayName("onEntryUpdatedでsyncEntryが呼ばれる")
        fun onEntryUpdatedCallsSyncEntry() = runTest(dispatcher) {
            // Given
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(emptyList())
            coEvery { librarySyncService.syncEntry(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.onEntryUpdated("entry1")

            // Then
            coVerify { librarySyncService.syncEntry("entry1") }
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
