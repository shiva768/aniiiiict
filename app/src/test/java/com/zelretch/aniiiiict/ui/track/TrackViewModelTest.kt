package com.zelretch.aniiiiict.ui.track

import android.content.Context
import app.cash.turbine.test
import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.domain.filter.AvailableFilters
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelTest : BehaviorSpec({
    val dispatcher = UnconfinedTestDispatcher()
    val filterStateFlow = MutableStateFlow(FilterState())
    val filterPreferences = mockk<FilterPreferences> {
        every { filterState } returns filterStateFlow
    }
    val loadProgramsUseCase = mockk<LoadProgramsUseCase>()
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    mockk<BulkRecordEpisodesUseCase>()
    val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
    val filterProgramsUseCase = mockk<FilterProgramsUseCase>()
    val judgeFinalUseCase = mockk<JudgeFinaleUseCase>()
    mockk<Context>(relaxed = true)
    lateinit var viewModel: TrackViewModel

    beforeTest {
        Dispatchers.setMain(dispatcher)
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
            judgeFinalUseCase
        )
        dispatcher.scheduler.advanceUntilIdle() // ViewModelのinitコルーチンを確実に進める
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("プログラム一覧ロード") {
        `when`("正常にロードできる場合") {
            then("UIStateにプログラムがセットされる") {
                runTest {
                    val fakePrograms = listOf<ProgramWithWork>(mockk(relaxed = true))
                    coEvery { loadProgramsUseCase.invoke() } returns flowOf(fakePrograms)
                    every { filterProgramsUseCase.invoke(any(), any()) } returns fakePrograms
                    every { filterProgramsUseCase.extractAvailableFilters(any()) } returns AvailableFilters(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList()
                    )
                    runTest(dispatcher) {
                        viewModel.uiState.test {
                            awaitItem() // 初期値を必ず受け取る
                            filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy"))
                            dispatcher.scheduler.advanceUntilIdle() // emitを確実に進める
                            awaitItem() // 状態遷移1: ローディングやfilter反映（isLoading=true）
                            awaitItem() // 状態遷移2: データ反映・ローディング完了（isLoading=false）
                            val updated = awaitItem() // 状態遷移3（必要なら）
                            updated.programs shouldBe fakePrograms
                            updated.isLoading shouldBe false
                            updated.error shouldBe null
                        }
                    }
                }
            }
        }
        `when`("例外が発生する場合") {
            then("UIStateにエラーがセットされる") {
                runTest(dispatcher) {
                    coEvery { loadProgramsUseCase.invoke() } returns flow {
                        throw LoadProgramsException("error")
                    }
                    runTest(dispatcher) {
                        viewModel.uiState.test {
                            awaitItem() // 初期値を必ず受け取る
                            filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy-error"))
                            dispatcher.scheduler.advanceUntilIdle() // emitを確実に進める
                            awaitItem() // 状態遷移1: ローディングやfilter反映
                            val errorState = awaitItem() // 状態遷移2: error反映
                            errorState.error shouldBe "処理中にエラーが発生しました"
                            errorState.isLoading shouldBe false
                        }
                    }
                }
            }
        }
    }
})

private class LoadProgramsException(message: String) : RuntimeException(message)
