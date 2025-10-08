package com.zelretch.aniiiiict.ui.watching

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WatchingScreenの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class WatchingScreenUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun watchingScreen_初期状態_基本要素が表示される() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val initialState = WatchingUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴中作品").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("戻る").assertIsDisplayed()
    }

    @Test
    fun watchingScreen_エラー状態_エラーメッセージが表示される() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val errorState = WatchingUiState(error = "ネットワークエラーが発生しました")
        every { mockViewModel.uiState } returns MutableStateFlow(errorState)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = errorState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ネットワークエラーが発生しました").assertIsDisplayed()
    }

    @Test
    fun watchingScreen_ローディング状態_ローディング表示が表示される() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val loadingState = WatchingUiState(isLoading = true)
        every { mockViewModel.uiState } returns MutableStateFlow(loadingState)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = loadingState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("読み込み中...").assertIsDisplayed()
    }

    @Test
    fun watchingScreen_エントリーが存在する_カードが表示される() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val entries = listOf(
            LibraryEntry(
                id = "entry1",
                work = Work(
                    id = "work1",
                    title = "天国大魔境",
                    seasonName = null,
                    seasonYear = 2023,
                    media = "TV",
                    malAnimeId = null,
                    viewerStatusState = StatusState.WATCHING,
                    image = null
                ),
                nextEpisode = Episode(
                    id = "ep1",
                    number = 9,
                    numberText = "第9話",
                    title = "学園の子供たち"
                ),
                statusState = StatusState.WATCHING
            )
        )
        val stateWithEntries = WatchingUiState(entries = entries, allEntries = entries)
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithEntries)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = stateWithEntries,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("天国大魔境").assertIsDisplayed()
        composeTestRule.onNodeWithText("2023年TV").assertIsDisplayed()
        composeTestRule.onNodeWithText("次：第9話 「学園の子供たち」").assertIsDisplayed()
    }

    @Test
    fun watchingScreen_フィルターが表示されている_ラジオボタンが表示される() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val stateWithFilter = WatchingUiState(isFilterVisible = true)
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithFilter)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = stateWithFilter,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("過去作のみ").assertIsDisplayed()
        composeTestRule.onNodeWithText("全作品").assertIsDisplayed()
    }

    @Test
    fun watchingScreen_フィルターボタンクリック_ViewModelメソッドが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val initialState = WatchingUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("フィルター").performClick()

        // Assert
        verify { mockViewModel.toggleFilterVisibility() }
    }

    @Test
    fun watchingScreen_戻るボタンクリック_コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<WatchingViewModel>(relaxed = true)
        val initialState = WatchingUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)
        var backPressed = false

        // Act
        composeTestRule.setContent {
            WatchingScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onNavigateBack = { backPressed = true }
            )
        }
        composeTestRule.onNodeWithContentDescription("戻る").performClick()

        // Assert
        assert(backPressed)
    }
}
