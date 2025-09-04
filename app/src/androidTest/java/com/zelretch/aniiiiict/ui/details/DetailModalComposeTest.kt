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
import io.mockk.coVerify
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

        // いずれかの選択肢が表示される（WATCHED を例に）
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()

        // 選択できることを確認
        composeTestRule.onNodeWithText("WATCHED").performClick()

        // 変更後の値が表示されるまで待機し検証（更新イベントで更新される）
        // 簡易的に再度クリック可能なことだけ確認
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()
    }

    @Test
    fun detailModal_一括視聴確認_ダイアログ表示と確認でonRefreshが呼ばれる() {
        val viewModel = createViewModel()

        // 検証用に2エピソードのProgramWithWorkを用意
        val work = Work(
            id = "work-2",
            title = "テストアニメ2",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val episode1 = Episode(id = "ep1", title = "第1話", numberText = "1", number = 1)
        val episode2 = Episode(id = "ep2", title = "第2話", numberText = "2", number = 2)
        val program1 = Program(
            id = "prog1",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = episode1
        )
        val program2 = Program(
            id = "prog2",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = episode2
        )
        val programWithWork = ProgramWithWork(
            programs = listOf(program1, program2),
            firstProgram = program1,
            work = work
        )

        var refreshed = false

        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = { refreshed = true }
            )
        }

        // ダイアログを表示（2話目までまとめて = index 1）
        viewModel.showConfirmDialog(1)

        // 確認ダイアログの文言を検証
        composeTestRule.onNodeWithText("ここまでまとめて視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("第2話まで、合計2話を視聴済みにします。\nこの操作は取り消せません。").assertIsDisplayed()
        composeTestRule.onNodeWithText("視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()

        // 確定を押下
        composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // 完了イベントにより onRefresh が呼ばれることを待つ
        composeTestRule.waitUntil(timeoutMillis = 5_000) { refreshed }
    }

    @Test
    fun detailModal_一括視聴確認_キャンセルでダイアログが閉じてonRefreshは呼ばれない() {
        val viewModel = createViewModel()

        // 2エピソードのProgramWithWorkを用意
        val work = Work(
            id = "work-3",
            title = "テストアニメ3",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val episode1 = Episode(id = "ep10", title = "第1話", numberText = "1", number = 1)
        val episode2 = Episode(id = "ep20", title = "第2話", numberText = "2", number = 2)
        val program1 = Program(
            id = "prog10",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = episode1
        )
        val program2 = Program(
            id = "prog20",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = episode2
        )
        val programWithWork = ProgramWithWork(
            programs = listOf(program1, program2),
            firstProgram = program1,
            work = work
        )

        var refreshed = false

        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = { refreshed = true }
            )
        }

        // ダイアログを表示（2話目まで = index 1）
        viewModel.showConfirmDialog(1)

        // ダイアログが表示されていることを確認
        composeTestRule.onNodeWithText("ここまでまとめて視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("第2話まで、合計2話を視聴済みにします。\nこの操作は取り消せません。").assertIsDisplayed()
        composeTestRule.onNodeWithText("視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()

        // キャンセルを押下
        composeTestRule.onNodeWithText("キャンセル").performClick()

        // 再描画を待ち、ダイアログが閉じていることを確認
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ここまでまとめて視聴済みにする").assertDoesNotExist()

        // onRefresh は呼ばれないこと
        org.junit.Assert.assertFalse(refreshed)
    }

    @Test
    fun detailModal_一括視聴確認_確認でRepository呼び出しをcoVerifyできる() {
        // Arrange: モックRepoとUseCase/VMを組み立て、Repo参照を保持する
        val repo = mockk<AnnictRepository>()
        coEvery { repo.updateWorkViewStatus(any(), any()) } returns true
        coEvery { repo.createRecord(any(), any()) } returns true
        val update = UpdateViewStateUseCase(repo)
        val watch = WatchEpisodeUseCase(repo, update)
        val bulk = BulkRecordEpisodesUseCase(watch)
        val viewModel = DetailModalViewModel(bulk, watch, update)

        // 2エピソードのProgramWithWork（WATCHING）
        val work = Work(
            id = "work-verify",
            title = "検証アニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val ep1 = Episode(id = "epA", title = "第1話", numberText = "1", number = 1)
        val ep2 = Episode(id = "epB", title = "第2話", numberText = "2", number = 2)
        val p1 = Program(id = "pA", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep1)
        val p2 = Program(id = "pB", startedAt = LocalDateTime.now(), channel = Channel("ch"), episode = ep2)
        val pw = ProgramWithWork(programs = listOf(p1, p2), firstProgram = p1, work = work)

        composeTestRule.setContent {
            DetailModal(
                programWithWork = pw,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Act: 確認ダイアログを表示し、確定をクリック
        viewModel.showConfirmDialog(1)
        composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert: createRecord が各エピソードで呼ばれる
        coVerify(exactly = 1) { repo.createRecord("epA", "work-verify") }
        coVerify(exactly = 1) { repo.createRecord("epB", "work-verify") }
        // WATCHING の場合は UpdateViewStateUseCase はステータス更新しないため、updateWorkViewStatus は呼ばれない
        coVerify(exactly = 0) { repo.updateWorkViewStatus(any(), any()) }
    }
}
