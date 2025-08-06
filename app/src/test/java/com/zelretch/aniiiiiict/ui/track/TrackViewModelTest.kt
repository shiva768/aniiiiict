package com.zelretch.aniiiiiict.ui.track

import android.content.Context
import app.cash.turbine.test
import com.zelretch.aniiiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiiict.domain.filter.AvailableFilters
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.usecase.*
import com.zelretch.aniiiiiict.util.Logger
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
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelTest : BehaviorSpec({
    val dispatcher = UnconfinedTestDispatcher()
    val filterStateFlow = MutableStateFlow(FilterState())
    val filterPreferences = mockk<FilterPreferences> {
        every { filterState } returns filterStateFlow
    }
    val loadProgramsUseCase = mockk<LoadProgramsUseCase>()
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val bulkRecordEpisodesUseCase = mockk<BulkRecordEpisodesUseCase>()
    val filterProgramsUseCase = mockk<FilterProgramsUseCase>()
    val anilistRepository = mockk<AniListRepository>()
    val judgeFinalUseCase = mockk<JudgeFinaleUseCase>()
    val logger = mockk<Logger>(relaxed = true)
    val context = mockk<Context>(relaxed = true)
    lateinit var viewModel: TrackViewModel
    lateinit var testScope: TestScope

    beforeTest {
        Dispatchers.setMain(dispatcher)
        testScope = TestScope(dispatcher)
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
            bulkRecordEpisodesUseCase,
            filterProgramsUseCase,
            filterPreferences,
            anilistRepository,
            judgeFinalUseCase,
            logger,
            context
        )
        viewModel.externalScope = testScope // テスト用スコープをセット
        testScope.testScheduler.advanceUntilIdle() // ViewModelのinitコルーチンを確実に進める
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
                            testScope.testScheduler.advanceUntilIdle() // emitを確実に進める
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
                        throw RuntimeException("error")
                    }
                    runTest(dispatcher) {
                        viewModel.uiState.test {
                            awaitItem() // 初期値を必ず受け取る
                            filterStateFlow.value = filterStateFlow.value.copy(selectedMedia = setOf("dummy-error"))
                            testScope.testScheduler.advanceUntilIdle() // emitを確実に進める
                            awaitItem() // 状態遷移1: ローディングやfilter反映
                            val errorState = awaitItem() // 状態遷移2: error反映
                            println("DEBUG: errorState = $errorState")
                            errorState.error shouldBe "error"
                            errorState.isLoading shouldBe false
                        }
                    }
                }
            }
        }
    }
})