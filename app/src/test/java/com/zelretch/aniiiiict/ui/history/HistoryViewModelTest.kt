package com.zelretch.aniiiiict.ui.history

import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiict.domain.usecase.SearchRecordsUseCase
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("HistoryViewModel")
open class HistoryViewModelTest {

    private lateinit var loadRecordsUseCase: LoadRecordsUseCase
    private lateinit var searchRecordsUseCase: SearchRecordsUseCase
    private lateinit var deleteRecordUseCase: DeleteRecordUseCase
    private lateinit var errorMapper: ErrorMapper
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        loadRecordsUseCase = mockk()
        searchRecordsUseCase = mockk()
        deleteRecordUseCase = mockk()
        errorMapper = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("初期化")
    inner class Initialization {

        @Test
        @DisplayName("loadRecordsが呼ばれUIステートが初期値で更新される")
        fun loadRecordsが呼ばれUIステートが初期値で更新される() = runTest(dispatcher) {
            // Given
            coEvery { loadRecordsUseCase(null) } returns Result.success(PaginatedRecords(emptyList(), false, null))
            every { searchRecordsUseCase(emptyList(), "") } returns emptyList()

            // When
            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            val state = viewModel.uiState.first { !it.isLoading }

            // Then
            assertEquals(emptyList<Record>(), state.records)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Nested
    @DisplayName("検索クエリ更新")
    inner class UpdateSearchQuery {

        @Test
        @DisplayName("クエリを渡すとsearchQueryとrecordsが更新される")
        fun withQuery() = runTest(dispatcher) {
            // Given
            val dummyRecords = listOf(
                mockk<Record> {
                    every { work } returns mockk<Work> { every { title } returns "dummy" }
                }
            )
            coEvery { loadRecordsUseCase(null) } returns Result.success(PaginatedRecords(dummyRecords, false, null))
            every { searchRecordsUseCase(dummyRecords, "foo") } returns dummyRecords
            every { searchRecordsUseCase(dummyRecords, "") } returns dummyRecords

            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.updateSearchQuery("foo")
            val state = viewModel.uiState.value

            // Then
            assertEquals("foo", state.searchQuery)
            assertEquals(dummyRecords, state.records)
        }
    }

    @Nested
    @DisplayName("レコード削除")
    inner class DeleteRecord {

        @Test
        @DisplayName("レコードIDを渡すとrecordsとallRecordsから削除される")
        fun byId() = runTest(dispatcher) {
            // Given
            val record = mockk<Record> {
                every { id } returns "id1"
                every { work } returns mockk<Work> { every { title } returns "dummy" }
            }
            coEvery { loadRecordsUseCase(null) } returns Result.success(PaginatedRecords(listOf(record), false, null))
            every { searchRecordsUseCase(listOf(record), "") } returns listOf(record)
            every { searchRecordsUseCase(emptyList(), "") } returns emptyList()
            coEvery { deleteRecordUseCase("id1") } returns Result.success(Unit)

            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.deleteRecord("id1")
            val state = viewModel.uiState.first {
                !it.isLoading && it.allRecords.isEmpty()
            }

            // Then
            assertEquals(emptyList<Record>(), state.allRecords)
            assertEquals(emptyList<Record>(), state.records)
        }

        @Test
        @DisplayName("検索クエリ有効時にレコード削除するとフィルタリングが適用される")
        fun withActiveSearch() = runTest(dispatcher) {
            // Given
            val record1 = mockk<Record> {
                every { id } returns "id1"
                every { work.title } returns "Anime A"
            }
            val record2 = mockk<Record> {
                every { id } returns "id2"
                every { work.title } returns "Anime B"
            }
            coEvery { loadRecordsUseCase(null) } returns Result.success(
                PaginatedRecords(
                    listOf(record1, record2),
                    false,
                    null
                )
            )
            every { searchRecordsUseCase(listOf(record1, record2), "") } returns listOf(record1, record2)
            every { searchRecordsUseCase(listOf(record1, record2), "Anime") } returns listOf(record1, record2)
            every { searchRecordsUseCase(listOf(record2), "Anime") } returns listOf(record2)
            coEvery { deleteRecordUseCase("id1") } returns Result.success(Unit)

            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            viewModel.uiState.first { !it.isLoading }
            viewModel.updateSearchQuery("Anime")

            // When
            viewModel.deleteRecord("id1")

            val state = viewModel.uiState.first {
                !it.isLoading && it.allRecords.size == 1
            }

            // Then
            assertEquals(listOf(record2), state.allRecords)
            assertEquals(listOf(record2), state.records)
        }
    }

    @Nested
    @DisplayName("次ページロード")
    inner class LoadNextPage {

        @Test
        @DisplayName("hasNextPageがtrueで追加レコードが加わる")
        fun hasNextPageがtrueで追加レコードが加わる() = runTest(dispatcher) {
            // Given
            val record1 = mockk<Record> {
                every { id } returns "id1"
                every { work } returns mockk<Work> { every { title } returns "dummy1" }
            }
            val record2 = mockk<Record> {
                every { id } returns "id2"
                every { work } returns mockk<Work> { every { title } returns "dummy2" }
            }
            coEvery { loadRecordsUseCase(null) } returns
                Result.success(PaginatedRecords(listOf(record1), true, "cursor"))
            coEvery { loadRecordsUseCase("cursor") } returns
                Result.success(PaginatedRecords(listOf(record2), false, null))
            every { searchRecordsUseCase(listOf(record1), "") } returns listOf(record1)
            every { searchRecordsUseCase(listOf(record1, record2), "") } returns listOf(record1, record2)

            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.loadNextPage()
            val state = viewModel.uiState.first {
                !it.isLoading && it.allRecords.size == 2
            }

            // Then
            assertEquals(listOf(record1, record2), state.allRecords)
            assertEquals(listOf(record1, record2), state.records)
            assertFalse(state.hasNextPage)
        }

        @Test
        @DisplayName("hasNextPageがfalseの場合loadRecordsUseCaseが呼ばれない")
        fun hasNextPageがfalseの場合loadRecordsUseCaseが呼ばれない() = runTest(dispatcher) {
            // Given
            val record1 = mockk<Record> {
                every { id } returns "id1"
                every { work } returns mockk<Work> { every { title } returns "dummy1" }
            }
            coEvery { loadRecordsUseCase(null) } returns
                Result.success(PaginatedRecords(listOf(record1), false, "cursor"))
            every { searchRecordsUseCase(listOf(record1), "") } returns listOf(record1)

            val viewModel = HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                errorMapper
            )
            val initialState = viewModel.uiState.first { !it.isLoading }

            // When
            viewModel.loadNextPage()

            val finalState = viewModel.uiState.value

            // Then
            assertEquals(initialState.allRecords, finalState.allRecords)
        }
    }
}
