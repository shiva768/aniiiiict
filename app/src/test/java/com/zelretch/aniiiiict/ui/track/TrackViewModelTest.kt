package com.zelretch.aniiiiict.ui.track

import android.content.Context
import app.cash.turbine.test
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.filter.AvailableFilters
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.LocalDateTime

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
    val judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()
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
            judgeFinaleUseCase
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

    given("フィナーレ判定ロジック") {
        `when`("最終話を記録し、確認ダイアログで『いいえ』を選択") {
            then("ステータスは更新されず、ダイアログが閉じる") {
                runTest {
                    // Arrange
                    val workId = "work-finale-no"
                    val episodeId = "ep-final-12-no"
                    val fakePrograms = createTestProgram(workId, episodeId)
                    coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
                    coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
                    coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                        com.zelretch.aniiiiict.domain.usecase.FinaleState.FINALE_CONFIRMED
                    )

                    // Act & Assert
                    viewModel.uiState.test {
                        skipItems(1) // 初期状態をスキップ
                        viewModel.refresh()

                        viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                        skipItems(1)
                        val finaleConfirmationState = awaitItem()
                        finaleConfirmationState.showFinaleConfirmationForWorkId shouldBe workId

                        // 「いいえ」を選択
                        viewModel.dismissFinaleConfirmation()

                        val finalState = awaitItem()
                        finalState.showFinaleConfirmationForWorkId shouldBe null

                        coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
                    }
                }
            }
        }

        `when`("フィナーレ判定に失敗した場合(総話数不明)") {
            then("フィナーレ確認は表示されず、ステータス更新もされない") {
                runTest {
                    // Arrange
                    val workId = "work-unk-ep"
                    val episodeId = "ep5"
                    val fakePrograms = createTestProgram(workId, episodeId)

                    coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
                    coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
                    coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                        com.zelretch.aniiiiict.domain.usecase.FinaleState.UNKNOWN
                    )

                    // Act & Assert
                    viewModel.uiState.test {
                        skipItems(1) // 初期状態をスキップ
                        viewModel.refresh()
                        viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                        skipItems(1)
                        // recordEpisode内のisLoading=true, falseの遷移を待つ
                        awaitItem().isLoading shouldBe true
                        val finalState = awaitItem()
                        finalState.isLoading shouldBe false

                        // フィナーレ確認が表示されていないことを確認
                        finalState.showFinaleConfirmationForWorkId shouldBe null

                        coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
                    }
                }
            }
        }

        `when`("フィナーレ判定に失敗した場合") {
            then("フィナーレ確認は表示されず、ステータス更新もされない") {
                runTest {
                    // Arrange
                    val workId = "work1"
                    val episodeId = "ep1"
                    val fakePrograms = createTestProgram(workId, episodeId)

                    coEvery { loadProgramsUseCase() } returns flowOf(listOf(fakePrograms))
                    coEvery { watchEpisodeUseCase.invoke(any(), any(), any()) } returns Result.success(Unit)
                    coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
                        com.zelretch.aniiiiict.domain.usecase.FinaleState.UNKNOWN
                    )

                    // Act & Assert
                    viewModel.uiState.test {
                        skipItems(1) // 初期状態をスキップ
                        viewModel.refresh()

                        viewModel.recordEpisode(episodeId, workId, StatusState.WATCHING)

                        // recordEpisode内のisLoading=true, falseの遷移を待つ
                        skipItems(1)
                        awaitItem().isLoading shouldBe true
                        val finalState = awaitItem()
                        finalState.isLoading shouldBe false

                        // フィナーレ確認が表示されていないことを確認
                        finalState.showFinaleConfirmationForWorkId shouldBe null

                        coVerify(exactly = 0) { updateViewStateUseCase.invoke(any(), any()) }
                    }
                }
            }
        }
    }
})

private fun createTestProgram(workId: String, episodeId: String): ProgramWithWork {
    val work = Work(
        id = workId,
        title = "Test Anime",
        seasonName = SeasonName.SPRING,
        seasonYear = 2024,
        media = "TV",
        mediaText = "TV",
        viewerStatusState = StatusState.WATCHING,
        malAnimeId = "123"
    )
    val episode = Episode(
        id = episodeId,
        title = "Episode Title",
        numberText = "1",
        number = 1
    )
    val program = Program(
        id = "prog1",
        startedAt = LocalDateTime.now(),
        channel = Channel(name = "Channel"),
        episode = episode
    )
    return ProgramWithWork(
        programs = listOf(program),
        firstProgram = program,
        work = work
    )
}

private class LoadProgramsException(message: String) : RuntimeException(message)
