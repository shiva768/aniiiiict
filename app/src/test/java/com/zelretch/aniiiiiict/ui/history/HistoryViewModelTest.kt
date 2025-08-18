package com.zelretch.aniiiiiict.ui.history

import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiiict.domain.usecase.RecordsResult
import com.zelretch.aniiiiiict.domain.usecase.SearchRecordsUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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

@OptIn(ExperimentalCoroutinesApi::class)
open class HistoryViewModelTest : BehaviorSpec({
    val loadRecordsUseCase = mockk<LoadRecordsUseCase>()
    val searchRecordsUseCase = mockk<SearchRecordsUseCase>()
    val deleteRecordUseCase = mockk<DeleteRecordUseCase>()
    val dispatcher = UnconfinedTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(dispatcher)
    }
    afterSpec {
        Dispatchers.resetMain()
    }

    Given("初期化時") {
        When("loadRecordsが呼ばれる") {
            Then("UIステートが初期値で更新される") {
                runTest(dispatcher) {
                    coEvery { loadRecordsUseCase.invoke(null) } returns
                        RecordsResult(emptyList(), false, null)
                    every { searchRecordsUseCase(emptyList(), "") } returns emptyList()

                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    val state = viewModel.uiState.first { !it.isLoading }
                    state.records shouldBe emptyList()
                    state.isLoading shouldBe false
                    state.error shouldBe null
                }
            }
        }
    }

    Given("updateSearchQuery呼び出し") {
        When("クエリを渡す") {
            Then("searchQueryとrecordsが更新される") {
                runTest(dispatcher) {
                    val dummyRecords = listOf(
                        mockk<Record> {
                            every { work } returns mockk<Work> { every { title } returns "dummy" }
                        }
                    )
                    coEvery { loadRecordsUseCase.invoke(null) } returns
                        RecordsResult(dummyRecords, false, null)
                    every { searchRecordsUseCase(dummyRecords, "foo") } returns dummyRecords
                    every { searchRecordsUseCase(dummyRecords, "") } returns dummyRecords

                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    viewModel.uiState.first { !it.isLoading }
                    viewModel.updateSearchQuery("foo")
                    val state = viewModel.uiState.value
                    state.searchQuery shouldBe "foo"
                    state.records shouldBe dummyRecords
                }
            }
        }
    }

    Given("deleteRecord呼び出し") {
        When("レコードIDを渡す") {
            Then("recordsとallRecordsから削除される") {
                runTest(dispatcher) {
                    val record = mockk<Record> {
                        every { id } returns "id1"
                        every { work } returns mockk<Work> { every { title } returns "dummy" }
                    }
                    coEvery { loadRecordsUseCase.invoke(null) } returns
                        RecordsResult(listOf(record), false, null)
                    every { searchRecordsUseCase(listOf(record), "") } returns listOf(record)
                    every { searchRecordsUseCase(emptyList(), "") } returns emptyList()
                    coEvery { deleteRecordUseCase("id1") } returns true
                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    viewModel.uiState.first { !it.isLoading }
                    viewModel.deleteRecord("id1")
                    val state = viewModel.uiState.first {
                        !it.isLoading && it.allRecords.isEmpty()
                    }
                    state.allRecords shouldBe emptyList()
                    state.records shouldBe emptyList()
                }
            }
        }
    }

    Given("loadNextPage呼び出し") {
        When("hasNextPage=true, endCursorあり") {
            Then("追加レコードがallRecords/recordsに加わる") {
                runTest(dispatcher) {
                    val record1 = mockk<Record> {
                        every { id } returns "id1"
                        every { work } returns mockk<Work> { every { title } returns "dummy1" }
                    }
                    val record2 = mockk<Record> {
                        every { id } returns "id2"
                        every { work } returns mockk<Work> { every { title } returns "dummy2" }
                    }
                    coEvery { loadRecordsUseCase.invoke(null) } returns
                        RecordsResult(listOf(record1), true, "cursor")
                    coEvery { loadRecordsUseCase.invoke("cursor") } returns
                        RecordsResult(listOf(record2), false, null)
                    every { searchRecordsUseCase(listOf(record1), "") } returns listOf(record1)
                    every { searchRecordsUseCase(listOf(record1, record2), "") } returns
                        listOf(record1, record2)

                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    viewModel.uiState.first { !it.isLoading }
                    viewModel.loadNextPage()
                    val state = viewModel.uiState.first {
                        !it.isLoading && it.allRecords.size == 2
                    }
                    state.allRecords shouldBe listOf(record1, record2)
                    state.records shouldBe listOf(record1, record2)
                    state.hasNextPage shouldBe false
                }
            }
        }

        When("hasNextPage=false") {
            Then("loadRecordsUseCaseが呼ばれない") {
                runTest(dispatcher) {
                    val record1 = mockk<Record> {
                        every { id } returns "id1"
                        every { work } returns mockk<Work> { every { title } returns "dummy1" }
                    }
                    coEvery { loadRecordsUseCase.invoke(null) } returns
                        RecordsResult(listOf(record1), false, "cursor")
                    every { searchRecordsUseCase(listOf(record1), "") } returns listOf(record1)

                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    val initialState = viewModel.uiState.first { !it.isLoading }

                    viewModel.loadNextPage()

                    val finalState = viewModel.uiState.value
                    finalState.allRecords shouldBe initialState.allRecords
                }
            }
        }
    }

    Given("deleteRecord呼び出し") {
        When("レコードIDを渡し、検索クエリが有効") {
            Then("recordsとallRecordsから削除され、フィルタリングが適用される") {
                runTest(dispatcher) {
                    val record1 = mockk<Record> {
                        every { id } returns "id1"
                        every { work.title } returns "Anime A"
                    }
                    val record2 = mockk<Record> {
                        every { id } returns "id2"
                        every { work.title } returns "Anime B"
                    }
                    coEvery { loadRecordsUseCase.invoke(null) } returns RecordsResult(
                        listOf(record1, record2),
                        false,
                        null
                    )
                    every { searchRecordsUseCase(listOf(record1, record2), "") } returns
                        listOf(record1, record2)
                    every { searchRecordsUseCase(listOf(record1, record2), "Anime") } returns
                        listOf(record1, record2)
                    every { searchRecordsUseCase(listOf(record2), "Anime") } returns listOf(record2)
                    coEvery { deleteRecordUseCase("id1") } returns true

                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase
                    )
                    viewModel.uiState.first { !it.isLoading }
                    viewModel.updateSearchQuery("Anime")
                    viewModel.deleteRecord("id1")

                    val state = viewModel.uiState.first {
                        !it.isLoading && it.allRecords.size == 1
                    }
                    state.allRecords shouldBe listOf(record2)
                    state.records shouldBe listOf(record2)
                }
            }
        }
    }
})
