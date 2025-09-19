package com.zelretch.aniiiiict.ui.details

import app.cash.turbine.test
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DetailModalViewModelTest : BehaviorSpec({
    val bulkRecordEpisodesUseCase = mockk<BulkRecordEpisodesUseCase>()
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
    val judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()
    val dispatcher = UnconfinedTestDispatcher()
    lateinit var viewModel: DetailModalViewModel

    beforeTest {
        viewModel = DetailModalViewModel(
            bulkRecordEpisodesUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase,
            judgeFinaleUseCase
        )
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
                val episode = Episode(id = "ep-12", number = 12, title = "最終話")
                val program = Program(
                    id = "prog-12",
                    episode = episode,
                    channel = mockk(relaxed = true),
                    startedAt = mockk(relaxed = true)
                )
                val programWithWork = ProgramWithWork(
                    programs = listOf(program),
                    firstProgram = program,
                    work = work
                )

                // WatchEpisodeUseCaseのモック設定
                coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
                
                // JudgeFinaleUseCaseのモック設定（最終話判定）
                coEvery { judgeFinaleUseCase(12, 123) } returns mockk {
                    every { isFinale } returns true
                }

                runTest(dispatcher) {
                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.initialize(programWithWork)
                        awaitItem() // initialize後
                        
                        // エピソード記録実行
                        viewModel.recordEpisode("ep-12", StatusState.WATCHING)
                        
                        val updated = awaitItem()
                        updated.showSingleEpisodeFinaleConfirmation shouldBe true
                        updated.singleEpisodeFinaleNumber shouldBe 12
                        updated.singleEpisodeFinaleWorkId shouldBe "work-finale"
                    }
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
                val episode = Episode(id = "ep-10", number = 10, title = "第10話")
                val program = Program(
                    id = "prog-10",
                    episode = episode,
                    channel = mockk(relaxed = true),
                    startedAt = mockk(relaxed = true)
                )
                val programWithWork = ProgramWithWork(
                    programs = listOf(program),
                    firstProgram = program,
                    work = work
                )

                // WatchEpisodeUseCaseのモック設定
                coEvery { watchEpisodeUseCase(any(), any(), any()) } returns Result.success(Unit)
                
                // JudgeFinaleUseCaseのモック設定（非最終話判定）
                coEvery { judgeFinaleUseCase(10, 123) } returns mockk {
                    every { isFinale } returns false
                }

                runTest(dispatcher) {
                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.initialize(programWithWork)
                        awaitItem() // initialize後
                        
                        // エピソード記録実行
                        viewModel.recordEpisode("ep-10", StatusState.WATCHING)
                        
                        val updated = awaitItem()
                        updated.showSingleEpisodeFinaleConfirmation shouldBe false
                        updated.singleEpisodeFinaleNumber shouldBe null
                        updated.singleEpisodeFinaleWorkId shouldBe null
                    }
                }
            }
        }
    }
})
