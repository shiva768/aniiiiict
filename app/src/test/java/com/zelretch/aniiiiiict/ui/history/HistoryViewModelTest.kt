package com.zelretch.aniiiiiict.ui.history

import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiiict.domain.usecase.RecordsResult
import com.zelretch.aniiiiiict.domain.usecase.SearchRecordsUseCase
import com.zelretch.aniiiiiict.util.Logger
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
    val logger = mockk<Logger>(relaxed = true)
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
                    coEvery { loadRecordsUseCase.invoke(any()) } answers {
                        println("[DEBUG] loadRecordsUseCase called with: ${it.invocation.args[0]}")
                        RecordsResult(emptyList(), false, null)
                    }
                    every { searchRecordsUseCase(any(), any()) } answers {
                        println("[DEBUG] searchRecordsUseCase called with: ${it.invocation.args[0]}")
                        firstArg()
                    }
                    coEvery { deleteRecordUseCase(any()) } returns true
                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase,
                        logger
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
                    val dummyRecords = listOf(mockk<Record>())
                    coEvery { loadRecordsUseCase.invoke(any()) } answers {
                        println("[DEBUG] loadRecordsUseCase called with: ${it.invocation.args[0]}")
                        RecordsResult(dummyRecords, false, null)
                    }
                    every { searchRecordsUseCase(any(), any()) } answers {
                        println("[DEBUG] searchRecordsUseCase called with: ${it.invocation.args[0]}")
                        firstArg()
                    }
                    coEvery { deleteRecordUseCase(any()) } returns true
                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase,
                        logger
                    )
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
                    val record = mockk<Record> { every { id } returns "id1" }
                    coEvery { loadRecordsUseCase.invoke(any()) } answers {
                        println("[DEBUG] loadRecordsUseCase called with: ${it.invocation.args[0]}")
                        RecordsResult(listOf(record), false, null)
                    }
                    every { searchRecordsUseCase(any(), any()) } answers {
                        println("[DEBUG] searchRecordsUseCase called with: ${it.invocation.args[0]}")
                        firstArg()
                    }
                    coEvery { deleteRecordUseCase("id1") } returns true
                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase,
                        logger
                    )
                    viewModel.deleteRecord("id1")
                    val state = viewModel.uiState.first { !it.isLoading }
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
                    val record1 = mockk<Record> { every { id } returns "id1" }
                    val record2 = mockk<Record> { every { id } returns "id2" }
                    coEvery { loadRecordsUseCase.invoke(any()) } answers {
                        val cursor = it.invocation.args[0] as String?
                        println("[DEBUG] loadRecordsUseCase called with: $cursor")
                        when (cursor) {
                            null -> RecordsResult(listOf(record1), true, "cursor")
                            "cursor" -> RecordsResult(listOf(record2), false, null)
                            else -> RecordsResult(emptyList(), false, null)
                        }
                    }
                    every { searchRecordsUseCase(any(), any()) } answers {
                        println("[DEBUG] searchRecordsUseCase called with: ${it.invocation.args[0]}")
                        firstArg()
                    }
                    coEvery { deleteRecordUseCase(any()) } returns true
                    val viewModel = HistoryViewModel(
                        loadRecordsUseCase,
                        searchRecordsUseCase,
                        deleteRecordUseCase,
                        logger
                    )
                    // 1ページ目ロード完了まで待つ
                    viewModel.uiState.first { !it.isLoading }
                    println("[DEBUG] before loadNextPage: ${viewModel.uiState.value}")
                    viewModel.loadNextPage()
                    // 2ページ目ロード完了まで待つ（allRecordsが2件になるまで）
                    val state = viewModel.uiState.first { !it.isLoading && it.allRecords.size == 2 }
                    println("[DEBUG] after loadNextPage: $state")
                    state.allRecords shouldBe listOf(record1, record2)
                    state.records shouldBe listOf(record1, record2)
                    state.hasNextPage shouldBe false
                }
            }
        }
    }
})
