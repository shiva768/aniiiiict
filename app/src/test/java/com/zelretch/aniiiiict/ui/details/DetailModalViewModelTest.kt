package com.zelretch.aniiiiict.ui.details

import app.cash.turbine.test
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class DetailModalViewModelTest : BehaviorSpec({
    val bulkRecordEpisodesUseCase = mockk<BulkRecordEpisodesUseCase>()
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
    val judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()
    val dispatcher = UnconfinedTestDispatcher()
    lateinit var viewModel: DetailModalViewModel

    beforeTest {
        Dispatchers.setMain(dispatcher)
        viewModel = DetailModalViewModel(
            bulkRecordEpisodesUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase,
            judgeFinaleUseCase
        )
    }

    afterTest {
        Dispatchers.resetMain()
    }

    Given("initialize呼び出し") {
        When("ProgramWithWorkを渡すとstateが更新される") {
            Then("stateが正しく更新される") {
                val program = mockk<Program>()
                val work = mockk<Work> {
                    every { viewerStatusState } returns StatusState.WATCHING
                    every { id } returns "work-id"
                    every { malAnimeId } returns "123"
                }
                val programWithWork = ProgramWithWork(
                    programs = listOf(program),
                    firstProgram = program,
                    work = work
                )
                runTest(dispatcher) {
                    viewModel.state.test {
                        awaitItem()
                        viewModel.initialize(programWithWork)
                        val updated = awaitItem()
                        updated.programs shouldBe listOf(program)
                        updated.selectedStatus shouldBe StatusState.WATCHING
                        updated.workId shouldBe "work-id"
                        updated.malAnimeId shouldBe "123"
                    }
                }
            }
        }
    }

    Given("showConfirmDialog呼び出し") {
        When("indexを渡すとダイアログが表示状態になる") {
            Then("ダイアログのstateが正しく更新される") {
                runTest(dispatcher) {
                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.showConfirmDialog(2)
                        val updated = awaitItem()
                        updated.showConfirmDialog shouldBe true
                        updated.selectedEpisodeIndex shouldBe 2
                    }
                }
            }
        }
    }

    Given("単一エピソード記録でのフィナーレ判定") {
        When("最終話を記録した場合") {
            Then("フィナーレ確認ダイアログが表示される") {
                // Arrange
                val work = mockk<Work> {
                    every { viewerStatusState } returns StatusState.WATCHING
                    every { id } returns "work-finale"
                    every { malAnimeId } returns "123"
                }
                val episode = mockk<com.zelretch.aniiiiict.data.model.Episode> {
                    every { id } returns "ep-12"
                    every { number } returns 12
                    every { title } returns "最終話"
                }
                val program = mockk<Program> {
                    every { this@mockk.episode } returns episode
                    every { id } returns "prog-12"
                }
                val programWithWork = ProgramWithWork(
                    programs = listOf(program),
                    firstProgram = program,
                    work = work
                )

                // WatchEpisodeUseCaseのモック設定
                coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)

                // JudgeFinaleUseCaseのモック設定（最終話判定）
                coEvery {
                    judgeFinaleUseCase(12, 123)
                } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED)

                runTest(dispatcher) {
                    viewModel.initialize(programWithWork)

                    // エピソード記録実行
                    viewModel.recordEpisode("ep-12", StatusState.WATCHING)

                    // 状態確認
                    val state = viewModel.state.value
                    state.showSingleEpisodeFinaleConfirmation shouldBe true
                    state.singleEpisodeFinaleNumber shouldBe 12
                    state.singleEpisodeFinaleWorkId shouldBe "work-finale"
                }
            }
        }

        When("最終話ではないエピソードを記録した場合") {
            Then("フィナーレ確認ダイアログは表示されない") {
                // Arrange
                val work = mockk<Work> {
                    every { viewerStatusState } returns StatusState.WATCHING
                    every { id } returns "work-normal"
                    every { malAnimeId } returns "123"
                }
                val episode = mockk<com.zelretch.aniiiiict.data.model.Episode> {
                    every { id } returns "ep-10"
                    every { number } returns 10
                    every { title } returns "第10話"
                }
                val program = mockk<Program> {
                    every { this@mockk.episode } returns episode
                    every { id } returns "prog-10"
                }
                val programWithWork = ProgramWithWork(
                    programs = listOf(program),
                    firstProgram = program,
                    work = work
                )

                // WatchEpisodeUseCaseのモック設定
                coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)

                // JudgeFinaleUseCaseのモック設定（非最終話判定）
                coEvery {
                    judgeFinaleUseCase(10, 123)
                } returns JudgeFinaleResult(FinaleState.NOT_FINALE)

                runTest(dispatcher) {
                    viewModel.initialize(programWithWork)

                    // エピソード記録実行
                    viewModel.recordEpisode("ep-10", StatusState.WATCHING)

                    // 状態確認
                    val state = viewModel.state.value
                    state.showSingleEpisodeFinaleConfirmation shouldBe false
                    state.singleEpisodeFinaleNumber shouldBe null
                    state.singleEpisodeFinaleWorkId shouldBe null
                }
            }
        }
    }
})
