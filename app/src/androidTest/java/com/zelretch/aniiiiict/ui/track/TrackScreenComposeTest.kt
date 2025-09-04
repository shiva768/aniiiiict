package com.zelretch.aniiiiict.ui.track

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
 * TrackScreenのCompose UIテスト
 * 主要な画面要素の表示とユーザーインタラクションを検証
 */
@RunWith(AndroidJUnit4::class)
class TrackScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("番組一覧").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("履歴").assertIsDisplayed()
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
                onNavigateToHistory = {},
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
                onNavigateToHistory = {},
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
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("テストアニメ").assertIsDisplayed()
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
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithTag("finale_confirmation_snackbar").assertIsDisplayed()
        composeTestRule.onNodeWithText("このタイトルはエピソード12が最終話の可能性があります。\n視聴済みにしますか？")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("はい").assertIsDisplayed()
        composeTestRule.onNodeWithText("いいえ").assertIsDisplayed()
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
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("はい").performClick()

        // Assert
        verify { mockViewModel.confirmWatchedStatus() }
    }

    @Test
    fun trackScreen_履歴ナビゲーション_コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<TrackViewModel>(relaxed = true)
        val mockOnNavigateToHistory = mockk<() -> Unit>(relaxed = true)
        val initialState = TrackUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            TrackScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onRecordEpisode = { _, _, _ -> },
                onNavigateToHistory = mockOnNavigateToHistory,
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("履歴").performClick()

        // Assert
        verify { mockOnNavigateToHistory() }
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
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        // Dismiss snackbar by clicking "いいえ"
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
                onNavigateToHistory = {},
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

    // ==== 以下、Repository呼び出しをcoVerifyする統合的テスト ====
    private fun buildRealViewModelWithRepoMock():
        Pair<TrackViewModel, com.zelretch.aniiiiict.data.repository.AnnictRepository> {
        val repo = mockk<com.zelretch.aniiiiict.data.repository.AnnictRepository>()
        io.mockk.coEvery { repo.createRecord(any(), any()) } returns true
        io.mockk.coEvery { repo.updateWorkViewStatus(any(), any()) } returns true
        io.mockk.coEvery { repo.getRawProgramsData() } returns
            kotlinx.coroutines.flow.flowOf(emptyList())

        val load = com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase(repo)
        val update = com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase(repo)
        val watch = com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase(repo, update)
        val filter = com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase(
            com.zelretch.aniiiiict.domain.filter.ProgramFilter()
        )
        val judge = mockk<com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase>(relaxed = true)
        val prefs = mockk<com.zelretch.aniiiiict.data.datastore.FilterPreferences>(relaxed = true) {
            every { filterState } returns MutableStateFlow(
                com.zelretch.aniiiiict.domain.filter.FilterState()
            )
        }
        val vm = TrackViewModel(load, watch, update, filter, prefs, judge)
        return vm to repo
    }

    @Test
    fun trackScreen_エピソード記録_Repository呼び出しをcoVerifyできる() {
        // Arrange real VM + mocked repository
        val (viewModel, repo) = buildRealViewModelWithRepoMock()

        val work = Work(
            id = "work-verify",
            title = "検証アニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val episode = Episode(id = "ep-verify", title = "第1話", numberText = "1", number = 1)
        val program = Program(
            id = "prog-verify",
            startedAt = LocalDateTime.now(),
            channel = Channel(name = "ch"),
            episode = episode
        )
        val pw = ProgramWithWork(programs = listOf(program), firstProgram = program, work = work)
        val state = TrackUiState(programs = listOf(pw))

        // Act: render with a callback that delegates to VM.recordEpisode (as production would)
        composeTestRule.setContent {
            TrackScreen(
                viewModel = viewModel,
                uiState = state,
                onRecordEpisode = { epId, wId, status -> viewModel.recordEpisode(epId, wId, status) },
                onNavigateToHistory = {},
                onRefresh = {}
            )
        }

        // Click the record button
        composeTestRule.onNodeWithContentDescription("記録する").performClick()

        // Assert repository.createRecord was called with episode/work IDs
        io.mockk.coVerify(exactly = 1) { repo.createRecord("ep-verify", "work-verify") }
    }
}
