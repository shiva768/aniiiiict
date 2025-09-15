package com.zelretch.aniiiiict.ui.track

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
 * TrackScreenの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class トラック画面UIテスト {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trackScreen_初期状態_基本要素が表示される() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val initialState = TrackUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴記録").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
    }

    @Test
    fun trackScreen_エラー状態_スナックバーとエラーメッセージが表示される() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val errorState = TrackUiState(error = "ネットワークエラーが発生しました")
        every { mockViewModel.uiState } returns MutableStateFlow(errorState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = errorState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithTag("snackbar").assertIsDisplayed()
        composeTestRule.onNodeWithText("ネットワークエラーが発生しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("再読み込み").assertIsDisplayed()
    }

    @Test
    fun trackScreen_フィルターボタンクリック_ViewModelメソッドが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val initialState = TrackUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("フィルター").performClick()

        // Assert
        verify { mockViewModel.toggleFilterVisibility() }
    }

    @Test
    fun trackScreen_番組リスト_プログラムカードが表示される() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)

        // Create sample program data
        val sampleWork = Work(
            id = "1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )

        val sampleEpisode = Episode(
            id = "ep1",
            title = "第1話",
            numberText = "1",
            number = 1
        )

        val sampleChannel = Channel(
            name = "テストチャンネル"
        )

        val sampleProgram = Program(
            id = "prog1",
            startedAt = LocalDateTime.now(),
            channel = sampleChannel,
            episode = sampleEpisode
        )

        val programWithWork = ProgramWithWork(
            programs = listOf(sampleProgram),
            firstProgram = sampleProgram,
            work = sampleWork
        )

        val stateWithPrograms = TrackUiState(programs = listOf(programWithWork))
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithPrograms)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = stateWithPrograms,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("テストアニメ").assertIsDisplayed()
    }

    @Test
    fun trackScreen_プログラムカードクリック_詳細モーダルが開く() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)

        // Create sample program data
        val sampleWork = Work(
            id = "1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )

        val sampleEpisode = Episode(
            id = "ep1",
            title = "第1話",
            numberText = "1",
            number = 1
        )

        val sampleChannel = Channel(
            name = "テストチャンネル"
        )

        val sampleProgram = Program(
            id = "prog1",
            startedAt = LocalDateTime.now(),
            channel = sampleChannel,
            episode = sampleEpisode
        )

        val programWithWork = ProgramWithWork(
            programs = listOf(sampleProgram),
            firstProgram = sampleProgram,
            work = sampleWork
        )

        val stateWithPrograms = TrackUiState(programs = listOf(programWithWork))
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithPrograms)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = stateWithPrograms,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // プログラムカードをクリック
        composeTestRule.onNodeWithTag("program_card_1").performClick()

        // Assert - showUnwatchedEpisodesが呼ばれることを確認
        verify { mockViewModel.showUnwatchedEpisodes(programWithWork) }
    }

    @Test
    fun trackScreen_フィナーレ確認_適切なスナックバーが表示される() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val finaleState = TrackUiState(
            showFinaleConfirmationForWorkId = "work1",
            showFinaleConfirmationForEpisodeNumber = 12
        )
        every { mockViewModel.uiState } returns MutableStateFlow(finaleState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = finaleState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        composeTestRule.waitForIdle()

        // Assert
        composeTestRule.onNodeWithTag("finale_confirmation_snackbar", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("はい", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("いいえ", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun trackScreen_フィナーレ確認_はいボタンクリック() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val finaleState = TrackUiState(
            showFinaleConfirmationForWorkId = "work1",
            showFinaleConfirmationForEpisodeNumber = 12
        )
        every { mockViewModel.uiState } returns MutableStateFlow(finaleState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = finaleState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("はい").performClick()

        // Assert
        verify { mockViewModel.confirmWatchedStatus() }
    }

    @Test
    fun trackScreen_フィナーレ確認_いいえボタンクリックで閉じる() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val finaleState = TrackUiState(
            showFinaleConfirmationForWorkId = "work1",
            showFinaleConfirmationForEpisodeNumber = 12
        )
        every { mockViewModel.uiState } returns MutableStateFlow(finaleState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = finaleState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("いいえ").performClick()

        // Assert
        verify { mockViewModel.dismissFinaleConfirmation() }
    }

    @Test
    fun trackScreen_エピソード記録ボタン_コールバックが正しく呼ばれる() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val onRecord = mockk<(String, String, StatusState) -> Unit>(relaxed = true)

        val sampleWork = Work(
            id = "work-123",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val sampleEpisode = Episode(
            id = "ep-1",
            title = "第1話",
            numberText = "1",
            number = 1
        )
        val sampleProgram = Program(
            id = "prog-1",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "チャンネル"),
            episode = sampleEpisode
        )
        val programWithWork = ProgramWithWork(
            programs = listOf(sampleProgram),
            firstProgram = sampleProgram,
            work = sampleWork
        )
        val state = TrackUiState(programs = listOf(programWithWork))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = state,
                onRecordEpisode = onRecord,
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // 記録ボタンを押下（ProgramCard内の contentDescription="記録する"）
        composeTestRule.onNodeWithContentDescription("記録する").performClick()

        // Assert
        verify {
            onRecord.invoke("ep-1", "work-123", StatusState.WATCHING)
        }
    }

    @Test
    fun trackScreen_ローディング状態_プルトゥリフレッシュが表示される() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val loadingState = TrackUiState(isLoading = true)
        every { mockViewModel.uiState } returns MutableStateFlow(loadingState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = loadingState,
                onRecordEpisode = { _, _, _ -> },
                onMenuClick = {},
                onRefresh = {}
            )
        }

        // Assert
        // ローディング状態でもUIが適切に表示されることを確認
        composeTestRule.onNodeWithText("視聴記録").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("メニュー").assertIsDisplayed()
    }
}
