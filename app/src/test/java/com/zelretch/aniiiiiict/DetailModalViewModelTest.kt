package com.zelretch.aniiiiiict.ui.details

import app.cash.turbine.test
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiiict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
open class DetailModalViewModelTest : BehaviorSpec({
    val bulkRecordEpisodesUseCase = mockk<BulkRecordEpisodesUseCase>()
    val watchEpisodeUseCase = mockk<WatchEpisodeUseCase>()
    val updateViewStateUseCase = mockk<UpdateViewStateUseCase>()
    val dispatcher = UnconfinedTestDispatcher()
    lateinit var viewModel: DetailModalViewModel
    lateinit var testScope: TestScope

    beforeTest {
        viewModel = DetailModalViewModel(
            bulkRecordEpisodesUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase
        )
        testScope = TestScope(dispatcher)
    }

    Given("initialize呼び出し") {
        When("ProgramWithWorkを渡すとstateが更新される") {
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

    Given("showConfirmDialog呼び出し") {
        When("indexを渡すとダイアログが表示状態になる") {
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
})
