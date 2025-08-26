package com.zelretch.aniiiiict.ui.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.domain.usecase.BulkRecordEpisodesUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * DetailModal のCompose UIテスト
 * ステータス変更ドロップダウン等、重要要素の破損検知を目的とする
 */
@RunWith(AndroidJUnit4::class)
class DetailModalComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): DetailModalViewModel {
        val repo = mockk<AnnictRepository>()
        coEvery { repo.updateWorkViewStatus(any(), any()) } returns true
        coEvery { repo.createRecord(any(), any()) } returns true
        val update = UpdateViewStateUseCase(repo)
        val watch = WatchEpisodeUseCase(repo, update)
        val bulk = BulkRecordEpisodesUseCase(watch)
        return DetailModalViewModel(bulk, watch, update)
    }

    private fun sampleProgramWithWork(status: StatusState = StatusState.WATCHING): ProgramWithWork {
        val work = Work(
            id = "work-1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = status
        )
        val episode = Episode(
            id = "ep1",
            title = "第1話",
            numberText = "1",
            number = 1
        )
        val program = Program(
            id = "prog1",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = episode
        )
        return ProgramWithWork(
            programs = listOf(program),
            firstProgram = program,
            work = work
        )
    }

    @Test
    fun detailModal_基本要素_タイトルと閉じるボタンが表示される() {
        val viewModel = createViewModel()
        val programWithWork = sampleProgramWithWork()

        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("未視聴エピソード (1件)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("閉じる").assertIsDisplayed()
    }

    @Test
    fun detailModal_ステータスドロップダウン_展開して選択できる() {
        val viewModel = createViewModel()
        val programWithWork = sampleProgramWithWork(StatusState.WATCHING)

        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // 初期選択状態が表示されている
        composeTestRule.onNodeWithText("WATCHING").assertIsDisplayed()

        // クリックでメニュー展開
        composeTestRule.onNodeWithText("WATCHING").performClick()

        // いずれかの選択肢が表示される（COMPLETED を例に）
        composeTestRule.onNodeWithText("COMPLETED").assertIsDisplayed()

        // 選択できることを確認
        composeTestRule.onNodeWithText("COMPLETED").performClick()

        // 変更後の値が表示されるまで待機し検証（更新イベントで更新される）
        // 簡易的に再度クリック可能なことだけ確認
        composeTestRule.onNodeWithText("COMPLETED").assertIsDisplayed()
    }
}
