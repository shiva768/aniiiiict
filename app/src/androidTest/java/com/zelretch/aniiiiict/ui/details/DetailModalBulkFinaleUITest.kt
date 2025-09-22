package com.zelretch.aniiiiict.ui.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * バルク記録時のフィナーレ確認ダイアログのUIテスト。
 * ViewModelをモック化し、フィナーレ確認状態での
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class DetailModalBulkFinaleUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createMockViewModelWithFinaleConfirmation(): DetailModalViewModel =
        mockk<DetailModalViewModel>(relaxed = true).apply {
            val mockEvents = MutableSharedFlow<DetailModalEvent>()
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = listOf(
                        createProgram("ep12", 12, "最終話")
                    ),
                    selectedStatus = StatusState.WATCHING,
                    workId = "work-1",
                    malAnimeId = "123",
                    showFinaleConfirmation = true,
                    finaleEpisodeNumber = 12
                )
            )
            every { events } returns mockEvents
            // Mock initialize to do nothing so it doesn't override our state
            every { initialize(any()) } just Runs
            // Mock other methods that might be called
            every { confirmFinaleWatched() } just Runs
            every { hideFinaleConfirmation() } just Runs
        }

    private fun createMockViewModelWithoutFinaleConfirmation(): DetailModalViewModel =
        mockk<DetailModalViewModel>(relaxed = true).apply {
            val mockEvents = MutableSharedFlow<DetailModalEvent>()
            every { state } returns MutableStateFlow(
                DetailModalState(
                    programs = listOf(
                        createProgram("ep10", 10, "第10話")
                    ),
                    selectedStatus = StatusState.WATCHING,
                    workId = "work-1",
                    malAnimeId = "123",
                    showFinaleConfirmation = false
                )
            )
            every { events } returns mockEvents
            // Mock initialize to do nothing so it doesn't override our state
            every { initialize(any()) } just Runs
        }

    private fun createProgram(id: String, number: Int, title: String): Program = Program(
        id = "prog-$id",
        episode = Episode(id = id, number = number, title = title, numberText = number.toString()),
        channel = Channel(name = "テストチャンネル"),
        startedAt = LocalDateTime.now()
    )

    private fun sampleProgramWithWork(): ProgramWithWork {
        val work = Work(
            id = "work-1",
            title = "テストアニメ",
            seasonName = SeasonName.AUTUMN,
            seasonYear = 2024,
            viewerStatusState = StatusState.WATCHING,
            malAnimeId = "123"
        )
        val programs = listOf(
            createProgram("ep11", 11, "第11話"),
            createProgram("ep12", 12, "最終話")
        )
        return ProgramWithWork(
            programs = programs,
            firstProgram = programs.first(),
            work = work
        )
    }

    @Test
    fun detailModal_フィナーレ確認ダイアログ_表示されること() {
        // Arrange
        val viewModel = createMockViewModelWithFinaleConfirmation()
        val programWithWork = sampleProgramWithWork()

        // Act
        composeTestRule.setContent {
            DetailModal(
                viewModel = viewModel,
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                onRefresh = {}
            )
        }

        // Compose UIの初期化を待機
        composeTestRule.waitForIdle()

        // Assert - フィナーレ確認ダイアログがすぐに表示されることを確認
        composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
        composeTestRule.onNodeWithText("第12話は最終話です。", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("視聴完了にする").assertIsDisplayed()
        composeTestRule.onNodeWithText("後で").assertIsDisplayed()
    }

    @Test
    fun detailModal_フィナーレ確認ダイアログ_視聴完了ボタンクリック() {
        // Arrange
        val viewModel = createMockViewModelWithFinaleConfirmation()
        val programWithWork = sampleProgramWithWork()

        // Act
        composeTestRule.setContent {
            DetailModal(
                viewModel = viewModel,
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                onRefresh = {}
            )
        }

        // フィナーレ確認ダイアログが表示されていることを確認
        composeTestRule.onNodeWithText("視聴完了にする").assertIsDisplayed()
        // 視聴完了ボタンをクリック
        composeTestRule.onNodeWithText("視聴完了にする").performClick()

        // Assert
        verify { viewModel.confirmFinaleWatched() }
    }

    @Test
    fun detailModal_フィナーレ確認ダイアログ_後でボタンクリック() {
        // Arrange
        val viewModel = createMockViewModelWithFinaleConfirmation()
        val programWithWork = sampleProgramWithWork()

        // Act
        composeTestRule.setContent {
            DetailModal(
                viewModel = viewModel,
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                onRefresh = {}
            )
        }

        // フィナーレ確認ダイアログが表示されていることを確認
        composeTestRule.onNodeWithText("後で").assertIsDisplayed()
        // 後でボタンをクリック
        composeTestRule.onNodeWithText("後で").performClick()

        // Assert
        verify { viewModel.hideFinaleConfirmation() }
    }

    @Test
    fun detailModal_フィナーレ確認なし_ダイアログ表示されないこと() {
        // Arrange
        val viewModel = createMockViewModelWithoutFinaleConfirmation()
        val programWithWork = sampleProgramWithWork()

        // Act
        composeTestRule.setContent {
            DetailModal(
                viewModel = viewModel,
                programWithWork = programWithWork,
                isLoading = false,
                onDismiss = {},
                onRefresh = {}
            )
        }

        // Assert - フィナーレ確認ダイアログは表示されない
        composeTestRule.onNodeWithText("最終話確認").assertDoesNotExist()
        composeTestRule.onNodeWithText("視聴完了にする").assertDoesNotExist()
    }
}
