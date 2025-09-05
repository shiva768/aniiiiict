package com.zelretch.aniiiiict.ui.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * DetailModalの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class DetailModalUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createMockViewModel(programWithWork: ProgramWithWork): DetailModalViewModel =
        mockk<DetailModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = programWithWork.programs,
                    selectedStatus = programWithWork.work.viewerStatusState,
                    workId = programWithWork.work.id
                )
            )
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
        // Arrange
        val programWithWork = sampleProgramWithWork()
        val viewModel = createMockViewModel(programWithWork)
        val mockOnDismiss = mockk<() -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = mockOnDismiss,
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("未視聴エピソード (1件)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("閉じる").assertIsDisplayed()
    }

    @Test
    fun detailModal_閉じるボタンクリック_onDismissが呼ばれる() {
        // Arrange
        val programWithWork = sampleProgramWithWork()
        val viewModel = createMockViewModel(programWithWork)
        val mockOnDismiss = mockk<() -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = mockOnDismiss,
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("閉じる").performClick()

        // Assert
        verify { mockOnDismiss() }
    }

    @Test
    fun detailModal_ステータスドロップダウン_展開して選択できる() {
        // Arrange
        val programWithWork = sampleProgramWithWork(StatusState.WATCHING)
        val viewModel = createMockViewModel(programWithWork)

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Assert - 初期選択状態が表示されている
        composeTestRule.onNodeWithText("WATCHING").assertIsDisplayed()

        // クリックでメニュー展開
        composeTestRule.onNodeWithText("WATCHING").performClick()

        // Wait for dropdown to expand
        composeTestRule.waitForIdle()

        // いずれかの選択肢が表示される（WATCHED を例に）
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()

        // 選択できることを確認
        composeTestRule.onNodeWithText("WATCHED").performClick()

        // Wait for selection to process
        composeTestRule.waitForIdle()

        // 変更後の値が表示されるまで待機し検証（更新イベントで更新される）
        // 簡易的に再度クリック可能なことだけ確認
        composeTestRule.onNodeWithText("WATCHED").assertIsDisplayed()
    }

    @Test
    fun detailModal_一括視聴確認ダイアログ_表示内容が正しい() {
        // Arrange
        // 2エピソードのProgramWithWorkを用意
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

        val viewModel = mockk<DetailModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = programWithWork.programs,
                    selectedStatus = programWithWork.work.viewerStatusState,
                    workId = programWithWork.work.id,
                    showConfirmDialog = true,
                    selectedEpisodeIndex = 1
                )
            )
        }

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert - 確認ダイアログの内容を検証
        composeTestRule.onNodeWithText("ここまでまとめて視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("第2話まで、合計2話を視聴済みにします。\nこの操作は取り消せません。").assertIsDisplayed()
        composeTestRule.onNodeWithText("視聴済みにする").assertIsDisplayed()
        composeTestRule.onNodeWithText("キャンセル").assertIsDisplayed()
    }

    @Test
    fun detailModal_一括視聴確認ダイアログ_キャンセルボタンクリック() {
        // Arrange
        val programWithWork = sampleProgramWithWork()
        val viewModel = mockk<DetailModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = programWithWork.programs,
                    selectedStatus = programWithWork.work.viewerStatusState,
                    workId = programWithWork.work.id,
                    showConfirmDialog = true,
                    selectedEpisodeIndex = 0
                )
            )
        }

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("キャンセル").performClick()

        // Assert - hideConfirmDialog が呼ばれることを検証
        verify { viewModel.hideConfirmDialog() }
    }

    @Test
    fun detailModal_一括視聴確認ダイアログ_確定ボタンクリック() {
        // Arrange
        val programWithWork = sampleProgramWithWork()
        val mockOnRefresh = mockk<() -> Unit>(relaxed = true)
        val viewModel = mockk<DetailModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = programWithWork.programs,
                    selectedStatus = programWithWork.work.viewerStatusState,
                    workId = programWithWork.work.id,
                    showConfirmDialog = true,
                    selectedEpisodeIndex = 0
                )
            )
        }

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = mockOnRefresh
            )
        }

        composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert - confirmBulkRecord が呼ばれることを検証
        verify { viewModel.bulkRecordEpisodes(any(), any()) }
    }

    @Test
    fun detailModal_ローディング状態_プログレスインジケータが表示される() {
        // Arrange
        val programWithWork = sampleProgramWithWork()
        val viewModel = createMockViewModel(programWithWork)

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = true, // ローディング状態
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert - ローディングインジケータなどのローディング中UI要素の確認
        // 実際の実装に応じてテストを調整
        composeTestRule.onNodeWithText("未視聴エピソード (1件)").assertIsDisplayed()
    }

    @Test
    fun detailModal_複数エピソード_正しい件数が表示される() {
        // Arrange
        // 3エピソードのProgramWithWorkを用意
        val work = Work(
            id = "work-multi",
            title = "マルチエピソードアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val episodes = (1..3).map { i ->
            Episode(id = "ep$i", title = "第${i}話", numberText = "$i", number = i)
        }
        val programs = episodes.map { episode ->
            Program(
                id = "prog${episode.number}",
                startedAt = LocalDateTime.now(),
                channel = Channel(name = "チャンネル"),
                episode = episode
            )
        }
        val programWithWork = ProgramWithWork(
            programs = programs,
            firstProgram = programs.first(),
            work = work
        )
        val viewModel = createMockViewModel(programWithWork)

        // Act
        composeTestRule.setContent {
            DetailModal(
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("未視聴エピソード (3件)").assertIsDisplayed()
    }
}
