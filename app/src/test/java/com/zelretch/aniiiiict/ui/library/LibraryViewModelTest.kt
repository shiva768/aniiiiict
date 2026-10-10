package com.zelretch.aniiiiict.ui.library

import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkPriority
import com.zelretch.aniiiiict.domain.sync.LibrarySyncService
import com.zelretch.aniiiiict.domain.sync.SyncStatus
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.BulkRecordResult
import com.zelretch.aniiiiict.domain.usecase.FinaleJudgmentInfo
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadLibraryEntriesUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadUnwatchedEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.SetWorkPriorityUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
    private lateinit var watchEpisodeUseCase: WatchEpisodeUseCase
    private lateinit var loadUnwatchedEpisodesUseCase: LoadUnwatchedEpisodesUseCase
    private lateinit var bulkRecordEpisodesUseCase: BulkRecordEpisodesUseCase
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var updateViewStateUseCase: UpdateViewStateUseCase
    private lateinit var setWorkPriorityUseCase: SetWorkPriorityUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        loadLibraryEntriesUseCase = mockk()
        librarySyncService = mockk()
        coEvery { librarySyncService.advanceEntry(any(), any()) } returns Unit
        coEvery { librarySyncService.removeEntry(any()) } returns Unit
        coEvery { librarySyncService.applyStatus(any(), any()) } returns Unit
        watchEpisodeUseCase = mockk()
        loadUnwatchedEpisodesUseCase = mockk()
        bulkRecordEpisodesUseCase = mockk()
        judgeFinaleUseCase = mockk()
        updateViewStateUseCase = mockk()
        setWorkPriorityUseCase = mockk()
        coEvery { setWorkPriorityUseCase(any(), any()) } returns Result.success(Unit)
        errorMapper = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LibraryViewModel {
        every { librarySyncService.status } returns MutableStateFlow(SyncStatus.Idle)
        return LibraryViewModel(
            loadLibraryEntriesUseCase,
            librarySyncService,
            watchEpisodeUseCase,
            loadUnwatchedEpisodesUseCase,
            bulkRecordEpisodesUseCase,
            judgeFinaleUseCase,
            updateViewStateUseCase,
            setWorkPriorityUseCase,
            errorMapper
        )
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
            val viewModel =
                LibraryViewModel(
                    loadLibraryEntriesUseCase,
                    librarySyncService,
                    watchEpisodeUseCase,
                    loadUnwatchedEpisodesUseCase,
                    bulkRecordEpisodesUseCase,
                    judgeFinaleUseCase,
                    updateViewStateUseCase,
                    setWorkPriorityUseCase,
                    errorMapper
                )
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

            val viewModel =
                LibraryViewModel(
                    loadLibraryEntriesUseCase,
                    librarySyncService,
                    watchEpisodeUseCase,
                    loadUnwatchedEpisodesUseCase,
                    bulkRecordEpisodesUseCase,
                    judgeFinaleUseCase,
                    updateViewStateUseCase,
                    setWorkPriorityUseCase,
                    errorMapper
                )
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

        @Test
        @DisplayName("onWorkStatusChangedでworkIdから該当エントリーを引き、Annictから取り直さず手元に反映する")
        fun onWorkStatusChangedAppliesStatusLocally() = runTest(dispatcher) {
            // Given
            val entry = LibraryEntry(
                id = "entry1",
                work = createFakeWork("work1", "Work 1"),
                nextEpisode = null,
                statusState = StatusState.WATCHING
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { librarySyncService.syncEntry(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When: 詳細画面で work1 を中止にした
            viewModel.onWorkStatusChanged("work1", StatusState.STOP_WATCHING)

            // Then: workId から entry1 を引いて手元に反映する（再同期はしない）
            coVerify { librarySyncService.applyStatus("entry1", StatusState.STOP_WATCHING) }
            coVerify(exactly = 0) { librarySyncService.syncEntry(any()) }
            coVerify(exactly = 0) { librarySyncService.sync() }
        }

        @Test
        @DisplayName("onWorkStatusChangedで該当エントリーが無ければ何もしない")
        fun onWorkStatusChangedNoopWhenNotFound() = runTest(dispatcher) {
            // Given
            val entry = LibraryEntry(
                id = "entry1",
                work = createFakeWork("work1", "Work 1"),
                nextEpisode = null,
                statusState = StatusState.WATCHING
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { librarySyncService.syncEntry(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When: ライブラリに存在しない作品
            viewModel.onWorkStatusChanged("unknown-work", StatusState.STOP_WATCHING)

            // Then: 再同期は呼ばれない
            coVerify(exactly = 0) { librarySyncService.syncEntry(any()) }
            coVerify(exactly = 0) { librarySyncService.applyStatus(any(), any()) }
        }

        @Test
        @DisplayName("recordNextEpisodeで記録し、エピソード一覧から次の話へ進める（Annictから取り直さない）")
        fun recordNextEpisodeRecordsAndAdvances() = runTest(dispatcher) {
            // Given
            val entry = LibraryEntry(
                id = "entry1",
                work = createFakeWork("work1", "Work"),
                nextEpisode = Episode(id = "ep1", number = 1),
                statusState = StatusState.WATCHING
            )
            val ep2 = Episode(id = "ep2", number = 2)
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { loadUnwatchedEpisodesUseCase("work1", "ep1") } returns
                Result.success(listOf(Episode(id = "ep1", number = 1, hasNextEpisode = true), ep2))

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.recordNextEpisode(entry)

            // Then
            coVerify { watchEpisodeUseCase("ep1", "work1", StatusState.WATCHING) }
            coVerify { librarySyncService.advanceEntry("entry1", ep2) }
            coVerify(exactly = 0) { librarySyncService.syncEntry(any()) }
            assertNull(viewModel.uiState.value.recordingEntryId)
        }

        @Test
        @DisplayName("エピソード一覧が取れないときはAnnictから取り直す")
        fun recordNextEpisodeFallsBackToSync() = runTest(dispatcher) {
            // Given
            val entry = LibraryEntry(
                id = "entry1",
                work = createFakeWork("work1", "Work"),
                nextEpisode = Episode(id = "ep1", number = 1),
                statusState = StatusState.WATCHING
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { loadUnwatchedEpisodesUseCase(any(), any()) } returns Result.failure(RuntimeException("x"))
            coEvery { librarySyncService.syncEntry(any()) } returns Unit

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.recordNextEpisode(entry)

            // Then
            coVerify { librarySyncService.syncEntry("entry1") }
            coVerify(exactly = 0) { librarySyncService.advanceEntry(any(), any()) }
        }

        @Test
        @DisplayName("記録処理が完了する前に連打しても2回目は弾かれ記録は1度だけ実行される")
        fun recordNextEpisodeIgnoresDoubleTap() = runTest(dispatcher) {
            // Given - 1回目の記録が完了しないよう watchEpisodeUseCase を保留させる
            val entry = LibraryEntry(
                id = "entry1",
                work = createFakeWork("work1", "Work"),
                nextEpisode = Episode(id = "ep1", number = 1),
                statusState = StatusState.WATCHING
            )
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            val gate = CompletableDeferred<Unit>()
            coEvery { watchEpisodeUseCase(any(), any(), any()) } coAnswers {
                gate.await()
                Result.success(Unit)
            }
            coEvery { librarySyncService.syncEntry(any()) } returns Unit
            coEvery { loadUnwatchedEpisodesUseCase(any(), any()) } returns Result.success(emptyList())

            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            // When - 1回目は処理中のまま、続けて2回目をタップ
            viewModel.recordNextEpisode(entry)
            viewModel.recordNextEpisode(entry)
            gate.complete(Unit)

            // Then - 記録は1度だけ
            coVerify(exactly = 1) { watchEpisodeUseCase("ep1", "work1", StatusState.WATCHING) }
        }
    }

    @Nested
    @DisplayName("まとめて記録")
    inner class BulkRecord {

        private val entry = LibraryEntry(
            id = "entry1",
            work = Work(
                id = "work1",
                title = "Work",
                viewerStatusState = StatusState.WATCHING
            ),
            nextEpisode = Episode(id = "ep2", number = 2),
            statusState = StatusState.WATCHING
        )
        private val unwatched = listOf(
            Episode(id = "ep2", number = 2, hasNextEpisode = true),
            Episode(id = "ep3", number = 3, hasNextEpisode = true),
            Episode(id = "ep4", number = 4)
        )

        private suspend fun createLoadedViewModel(): LibraryViewModel {
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { loadUnwatchedEpisodesUseCase("work1", "ep2") } returns Result.success(unwatched)
            coEvery { librarySyncService.syncEntry(any()) } returns Unit
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }
            return viewModel
        }

        @Test
        @DisplayName("まとめてを開くと未視聴エピソード一覧が読み込まれる")
        fun toggleLoadsUnwatchedEpisodes() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.toggleBulkRecord(entry)

            assertEquals("entry1", viewModel.uiState.value.bulkRecordEntryId)
            assertEquals(BulkEpisodesState.Loaded(unwatched), viewModel.uiState.value.bulkEpisodes)
        }

        @Test
        @DisplayName("開いている状態でもう一度押すと閉じる")
        fun toggleTwiceCollapses() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()

            viewModel.toggleBulkRecord(entry)
            viewModel.toggleBulkRecord(entry)

            assertNull(viewModel.uiState.value.bulkRecordEntryId)
            assertNull(viewModel.uiState.value.bulkEpisodes)
        }

        @Test
        @DisplayName("一覧の取得に失敗するとエラー状態になる")
        fun toggleShowsErrorOnFailure() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            val error = RuntimeException("boom")
            coEvery { loadUnwatchedEpisodesUseCase(any(), any()) } returns Result.failure(error)
            every { errorMapper.toUserMessage(error, any()) } returns "取得失敗"

            viewModel.toggleBulkRecord(entry)

            assertEquals(BulkEpisodesState.Error("取得失敗"), viewModel.uiState.value.bulkEpisodes)
        }

        @Test
        @DisplayName("タップした話までまとめて記録され、開いている一覧から次の話へ進めて閉じる")
        fun bulkRecordUpToRecordsAndSyncs() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery {
                bulkRecordEpisodesUseCase(any(), any(), any(), any(), any())
            } returns Result.success(BulkRecordResult())
            viewModel.toggleBulkRecord(entry)

            viewModel.bulkRecordUpTo(entry, 1)

            coVerify {
                bulkRecordEpisodesUseCase(listOf("ep2", "ep3"), "work1", StatusState.WATCHING, any(), any())
            }
            coVerify { librarySyncService.advanceEntry("entry1", Episode(id = "ep4", number = 4)) }
            coVerify(exactly = 0) { librarySyncService.syncEntry(any()) }
            assertNull(viewModel.uiState.value.bulkRecordEntryId)
            assertNull(viewModel.uiState.value.recordingEntryId)
        }

        @Test
        @DisplayName("記録中に連打しても2回目は弾かれる")
        fun bulkRecordUpToIgnoresDoubleTap() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            val gate = CompletableDeferred<Unit>()
            coEvery { bulkRecordEpisodesUseCase(any(), any(), any(), any(), any()) } coAnswers {
                gate.await()
                Result.success(BulkRecordResult())
            }
            viewModel.toggleBulkRecord(entry)

            viewModel.bulkRecordUpTo(entry, 0)
            viewModel.bulkRecordUpTo(entry, 0)
            gate.complete(Unit)

            coVerify(exactly = 1) { bulkRecordEpisodesUseCase(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("記録に失敗してもエラーを出し、一部記録済みの可能性があるので再同期する")
        fun bulkRecordUpToFailureStillSyncs() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            val error = RuntimeException("boom")
            coEvery { bulkRecordEpisodesUseCase(any(), any(), any(), any(), any()) } returns Result.failure(error)
            every { errorMapper.toUserMessage(error, any()) } returns "記録失敗"
            viewModel.toggleBulkRecord(entry)

            viewModel.bulkRecordUpTo(entry, 2)

            coVerify { librarySyncService.syncEntry("entry1") }
            assertEquals("記録失敗", viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.recordingEntryId)
        }
    }

    @Nested
    @DisplayName("最終話確認")
    inner class Finale {

        private val entry = LibraryEntry(
            id = "entry1",
            work = Work(
                id = "work1",
                title = "Work",
                malAnimeId = "123",
                viewerStatusState = StatusState.WATCHING
            ),
            nextEpisode = Episode(id = "ep11", number = 11),
            statusState = StatusState.WATCHING
        )
        private val unwatched = listOf(
            Episode(id = "ep11", number = 11, hasNextEpisode = true),
            Episode(id = "ep12", number = 12)
        )

        private suspend fun createLoadedViewModel(): LibraryViewModel {
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(entry))
            coEvery { loadUnwatchedEpisodesUseCase("work1", "ep11") } returns Result.success(unwatched)
            coEvery { librarySyncService.syncEntry(any()) } returns Unit
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }
            return viewModel
        }

        @Test
        @DisplayName("まとめて記録で最終話まで記録すると視聴完了ダイアログが出る")
        fun bulkRecordToFinaleShowsConfirmation() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery { bulkRecordEpisodesUseCase(any(), any(), any(), any(), any()) } returns
                Result.success(BulkRecordResult(JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)))
            viewModel.toggleBulkRecord(entry)

            viewModel.bulkRecordUpTo(entry, 1)

            coVerify {
                bulkRecordEpisodesUseCase(
                    listOf("ep11", "ep12"),
                    "work1",
                    StatusState.WATCHING,
                    FinaleJudgmentInfo(malAnimeId = 123, lastEpisodeNumber = 12, lastEpisodeHasNext = false),
                    any()
                )
            }
            assertEquals(FinaleConfirmation("entry1", "work1", 12), viewModel.uiState.value.finaleConfirmation)
        }

        @Test
        @DisplayName("最終話でなければダイアログは出ない")
        fun bulkRecordNotFinaleShowsNothing() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery { bulkRecordEpisodesUseCase(any(), any(), any(), any(), any()) } returns
                Result.success(BulkRecordResult(JudgeFinaleResult(FinaleState.NOT_FINALE)))
            viewModel.toggleBulkRecord(entry)

            viewModel.bulkRecordUpTo(entry, 0)

            assertNull(viewModel.uiState.value.finaleConfirmation)
        }

        @Test
        @DisplayName("「見た」で最終話を記録すると視聴完了ダイアログが出る")
        fun recordNextEpisodeFinaleShowsConfirmation() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase(11, 123) } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)

            viewModel.recordNextEpisode(entry)

            assertEquals(FinaleConfirmation("entry1", "work1", 11), viewModel.uiState.value.finaleConfirmation)
        }

        @Test
        @DisplayName("視聴完了にすると WATCHED に更新し、ライブラリの対象外なので手元から消す")
        fun confirmFinaleUpdatesStatus() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase(11, 123) } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)
            coEvery { updateViewStateUseCase("work1", StatusState.WATCHED) } returns Result.success(Unit)
            viewModel.recordNextEpisode(entry)

            viewModel.confirmFinale()

            coVerify { updateViewStateUseCase("work1", StatusState.WATCHED) }
            coVerify { librarySyncService.advanceEntry("entry1", Episode(id = "ep12", number = 12)) }
            coVerify { librarySyncService.removeEntry("entry1") }
            coVerify(exactly = 0) { librarySyncService.syncEntry(any()) }
            assertNull(viewModel.uiState.value.finaleConfirmation)
        }

        @Test
        @DisplayName("後でを押すとダイアログが閉じ、ステータスは変えない")
        fun dismissFinaleKeepsStatus() = runTest(dispatcher) {
            val viewModel = createLoadedViewModel()
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { judgeFinaleUseCase(11, 123) } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)
            viewModel.recordNextEpisode(entry)

            viewModel.dismissFinale()

            assertNull(viewModel.uiState.value.finaleConfirmation)
            coVerify(exactly = 0) { updateViewStateUseCase(any(), any()) }
        }
    }

    @Nested
    @DisplayName("優先度")
    inner class Priority {

        private fun entry(id: String, title: String, priority: WorkPriority = WorkPriority.NONE) = LibraryEntry(
            id = "entry_$id",
            work = createFakeWork(id, title),
            nextEpisode = Episode(id = "ep_$id", number = 1),
            statusState = StatusState.WATCHING,
            priority = priority
        )

        @Test
        @DisplayName("Tier1 → Tier2 → Tier3 → 無印 の順に並び、セクション内はタイトル順のまま")
        fun sortsByPriorityKeepingInnerOrder() = runTest(dispatcher) {
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(
                listOf(
                    entry("w1", "D", WorkPriority.TIER3),
                    entry("w2", "C"),
                    entry("w3", "B", WorkPriority.TIER1),
                    entry("w4", "A"),
                    entry("w5", "E", WorkPriority.TIER1),
                    entry("w6", "F", WorkPriority.TIER2)
                )
            )
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            viewModel.updateSortOrder(LibrarySortOrder.TITLE_ASC)

            assertEquals(
                listOf("B", "E", "F", "D", "A", "C"),
                viewModel.uiState.value.entries.map { it.work.title }
            )
        }

        @Test
        @DisplayName("setPriority で保存して Room から読み直す")
        fun setPriorityPersistsAndReloads() = runTest(dispatcher) {
            val target = entry("w1", "A")
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(target))
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            viewModel.setPriority(target, WorkPriority.TIER1)

            coVerifyOrder {
                setWorkPriorityUseCase("w1", WorkPriority.TIER1)
                loadLibraryEntriesUseCase()
            }
        }

        @Test
        @DisplayName("同じ優先度を選んだときは何もしない")
        fun setSamePriorityDoesNothing() = runTest(dispatcher) {
            val target = entry("w1", "A", WorkPriority.TIER2)
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(target))
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            viewModel.setPriority(target, WorkPriority.TIER2)

            coVerify(exactly = 0) { setWorkPriorityUseCase(any(), any()) }
        }

        @Test
        @DisplayName("記録しても優先度は変わらない")
        fun recordNextEpisodeKeepsPriority() = runTest(dispatcher) {
            val target = entry("w1", "A", WorkPriority.TIER3)
            coEvery { loadLibraryEntriesUseCase() } returns Result.success(listOf(target))
            coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
            coEvery { librarySyncService.syncEntry(any()) } returns Unit
            coEvery { loadUnwatchedEpisodesUseCase(any(), any()) } returns Result.success(emptyList())
            val viewModel = createViewModel()
            viewModel.uiState.first { !it.isLoading }

            viewModel.recordNextEpisode(target)

            coVerify(exactly = 0) { setWorkPriorityUseCase(any(), any()) }
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
