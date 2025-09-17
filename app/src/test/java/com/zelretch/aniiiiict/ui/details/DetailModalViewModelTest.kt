package com.zelretch.aniiiiict.ui.details

import app.cash.turbine.test
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
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

    Given("バルク記録後のフィナーレ判定") {
        When("最終話でない場合") {
            Then("フィナーレ確認ダイアログは表示されない") {
                runTest(dispatcher) {
                    // Setup
                    val episode = mockk<Episode> {
                        every { id } returns "episode-1"
                        every { number } returns 10
                    }
                    val program = mockk<Program> {
                        every { episode } returns episode
                    }
                    val work = mockk<Work> {
                        every { id } returns "work-1"
                        every { malAnimeId } returns "123"
                        every { viewerStatusState } returns StatusState.WATCHING
                    }
                    val programWithWork = ProgramWithWork(
                        programs = listOf(program),
                        firstProgram = program,
                        work = work
                    )

                    // Mock the bulk recording use case
                    coEvery { 
                        bulkRecordEpisodesUseCase(any(), any(), any(), any())
                    } returns Result.success(Unit)

                    // Mock the finale judge use case to return false
                    coEvery { 
                        judgeFinaleUseCase(10, 123) 
                    } returns JudgeFinaleResult(FinaleState.NOT_FINALE, false)

                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.initialize(programWithWork)
                        awaitItem() // initialize後
                        
                        viewModel.bulkRecordEpisodes(listOf("episode-1"), StatusState.WATCHING)
                        
                        // Wait for bulk recording completion
                        val bulkStart = awaitItem()
                        bulkStart.isBulkRecording shouldBe true
                        
                        val bulkComplete = awaitItem() 
                        bulkComplete.isBulkRecording shouldBe false
                        
                        // No finale confirmation should be shown
                        bulkComplete.showFinaleConfirmationForWorkId shouldBe null
                        bulkComplete.showFinaleConfirmationForEpisodeNumber shouldBe null
                    }
                }
            }
        }

        When("最終話の場合") {
            Then("フィナーレ確認ダイアログが表示される") {
                runTest(dispatcher) {
                    // Setup
                    val episode = mockk<Episode> {
                        every { id } returns "episode-12"
                        every { number } returns 12
                    }
                    val program = mockk<Program> {
                        every { episode } returns episode
                    }
                    val work = mockk<Work> {
                        every { id } returns "work-1"
                        every { malAnimeId } returns "123"
                        every { viewerStatusState } returns StatusState.WATCHING
                    }
                    val programWithWork = ProgramWithWork(
                        programs = listOf(program),
                        firstProgram = program,
                        work = work
                    )

                    // Mock the bulk recording use case
                    coEvery { 
                        bulkRecordEpisodesUseCase(any(), any(), any(), any())
                    } returns Result.success(Unit)

                    // Mock the finale judge use case to return true
                    coEvery { 
                        judgeFinaleUseCase(12, 123) 
                    } returns JudgeFinaleResult(FinaleState.FINALE_CONFIRMED, true)

                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.initialize(programWithWork)
                        awaitItem() // initialize後
                        
                        viewModel.bulkRecordEpisodes(listOf("episode-12"), StatusState.WATCHING)
                        
                        // Wait for bulk recording completion
                        val bulkStart = awaitItem()
                        bulkStart.isBulkRecording shouldBe true
                        
                        val bulkComplete = awaitItem() 
                        bulkComplete.isBulkRecording shouldBe false
                        
                        // Wait for finale state update
                        val finaleConfirm = awaitItem()
                        finaleConfirm.showFinaleConfirmationForWorkId shouldBe "work-1"
                        finaleConfirm.showFinaleConfirmationForEpisodeNumber shouldBe 12
                    }
                }
            }
        }
        
        When("MAL IDが無い場合") {
            Then("フィナーレ確認ダイアログは表示されない") {
                runTest(dispatcher) {
                    // Setup
                    val episode = mockk<Episode> {
                        every { id } returns "episode-12"
                        every { number } returns 12
                    }
                    val program = mockk<Program> {
                        every { episode } returns episode
                    }
                    val work = mockk<Work> {
                        every { id } returns "work-1"
                        every { malAnimeId } returns null // No MAL ID
                        every { viewerStatusState } returns StatusState.WATCHING
                    }
                    val programWithWork = ProgramWithWork(
                        programs = listOf(program),
                        firstProgram = program,
                        work = work
                    )

                    // Mock the bulk recording use case
                    coEvery { 
                        bulkRecordEpisodesUseCase(any(), any(), any(), any())
                    } returns Result.success(Unit)

                    viewModel.state.test {
                        awaitItem() // 初期値
                        viewModel.initialize(programWithWork)
                        awaitItem() // initialize後
                        
                        viewModel.bulkRecordEpisodes(listOf("episode-12"), StatusState.WATCHING)
                        
                        // Wait for bulk recording completion
                        val bulkStart = awaitItem()
                        bulkStart.isBulkRecording shouldBe true
                        
                        val bulkComplete = awaitItem() 
                        bulkComplete.isBulkRecording shouldBe false
                        
                        // No finale confirmation should be shown due to missing MAL ID
                        bulkComplete.showFinaleConfirmationForWorkId shouldBe null
                        bulkComplete.showFinaleConfirmationForEpisodeNumber shouldBe null
                    }
                }
            }
        }
    }
})
