package com.zelretch.aniiiiict.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

/**
 * HistoryScreenのCompose UIテスト
 * 履歴画面の主要な機能とUI要素を検証
 */
@RunWith(AndroidJUnit4::class)
class HistoryScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun historyScreen_初期状態_基本要素が表示される() {
        // Arrange
        val initialState = HistoryUiState()

        val mockOnNavigateBack = mockk<() -> Unit>(relaxed = true)
        val mockOnRetry = mockk<() -> Unit>(relaxed = true)
        val mockOnDeleteRecord = mockk<(String) -> Unit>(relaxed = true)
        val mockOnRefresh = mockk<() -> Unit>(relaxed = true)
        val mockOnLoadNextPage = mockk<() -> Unit>(relaxed = true)
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = initialState,
                actions = HistoryScreenActions(
                    onNavigateBack = mockOnNavigateBack,
                    onRetry = mockOnRetry,
                    onDeleteRecord = mockOnDeleteRecord,
                    onRefresh = mockOnRefresh,
                    onLoadNextPage = mockOnLoadNextPage,
                    onSearchQueryChange = mockOnSearchQueryChange
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴履歴").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("戻る").assertIsDisplayed()
        composeTestRule.onNodeWithText("作品名で検索").assertIsDisplayed()
    }

    @Test
    fun historyScreen_空の状態_適切なメッセージが表示される() {
        // Arrange
        val emptyState = HistoryUiState(
            records = emptyList(),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = emptyState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴履歴がありません").assertIsDisplayed()
        composeTestRule.onNodeWithText("下にスワイプして更新").assertIsDisplayed()
    }

    @Test
    fun historyScreen_履歴データ_レコードが表示される() {
        // Arrange
        val sampleWork = Work(
            id = "work1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHED
        )

        val sampleEpisode = Episode(
            id = "episode1",
            title = "第1話　始まりの物語",
            numberText = "1",
            number = 1
        )

        val sampleRecord = Record(
            id = "record1",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = sampleEpisode,
            work = sampleWork
        )

        val stateWithRecords = HistoryUiState(
            records = listOf(sampleRecord),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithRecords,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("テストアニメ").assertIsDisplayed()
        composeTestRule.onNodeWithText("EP1 第1話　始まりの物語").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("削除").assertIsDisplayed()
    }

    @Test
    fun historyScreen_戻るボタンクリック_ナビゲーションコールバックが呼ばれる() {
        // Arrange
        val mockOnNavigateBack = mockk<() -> Unit>(relaxed = true)
        val initialState = HistoryUiState()

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = initialState,
                actions = HistoryScreenActions(
                    onNavigateBack = mockOnNavigateBack,
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("戻る").performClick()

        // Assert
        verify { mockOnNavigateBack() }
    }

    @Test
    fun historyScreen_検索入力_コールバックが呼ばれる() {
        // Arrange
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)
        val initialState = HistoryUiState()

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = initialState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = mockOnSearchQueryChange
                )
            )
        }

        composeTestRule.onNodeWithText("作品名で検索").performTextInput("テスト")

        // Assert
        verify { mockOnSearchQueryChange("テスト") }
    }

    @Test
    fun historyScreen_検索文字入力済み_クリアボタンが表示される() {
        // Arrange
        val stateWithSearchQuery = HistoryUiState(searchQuery = "テストクエリ")

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithSearchQuery,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("クリア").assertIsDisplayed()
    }

    @Test
    fun historyScreen_クリアボタンクリック_検索文字がクリアされる() {
        // Arrange
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)
        val stateWithSearchQuery = HistoryUiState(searchQuery = "テストクエリ")

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithSearchQuery,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = mockOnSearchQueryChange
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("クリア").performClick()

        // Assert
        verify { mockOnSearchQueryChange("") }
    }

    @Test
    fun historyScreen_削除ボタンクリック_削除コールバックが呼ばれる() {
        // Arrange
        val mockOnDeleteRecord = mockk<(String) -> Unit>(relaxed = true)

        val sampleWork = Work(
            id = "work1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHED
        )

        val sampleEpisode = Episode(
            id = "episode1",
            title = "第1話",
            numberText = "1",
            number = 1
        )

        val sampleRecord = Record(
            id = "record1",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = sampleEpisode,
            work = sampleWork
        )

        val stateWithRecords = HistoryUiState(records = listOf(sampleRecord))

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithRecords,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = mockOnDeleteRecord,
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("削除").performClick()

        // Assert
        verify { mockOnDeleteRecord("record1") }
    }

    @Test
    fun historyScreen_エラー状態_エラーメッセージと再試行ボタンが表示される() {
        // Arrange
        val errorState = HistoryUiState(
            records = emptyList(),
            error = "ネットワークエラーが発生しました",
            isLoading = false
        )

        val mockOnRetry = mockk<() -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = errorState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = mockOnRetry,
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ネットワークエラーが発生しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("再試行").assertIsDisplayed()
    }

    @Test
    fun historyScreen_再試行ボタンクリック_再試行コールバックが呼ばれる() {
        // Arrange
        val mockOnRetry = mockk<() -> Unit>(relaxed = true)
        val errorState = HistoryUiState(
            records = emptyList(),
            error = "エラーです",
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = errorState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = mockOnRetry,
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        composeTestRule.onNodeWithText("再試行").performClick()

        // Assert
        verify { mockOnRetry() }
    }

    @Test
    fun historyScreen_次のページあり_もっと見るボタンが表示される() {
        // Arrange
        val stateWithNextPage = HistoryUiState(
            records = listOf(), // レコードは空でも可
            hasNextPage = true,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithNextPage,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("もっと見る").assertIsDisplayed()
    }

    @Test
    fun historyScreen_もっと見るボタンクリック_次ページ読み込みコールバックが呼ばれる() {
        // Arrange
        val mockOnLoadNextPage = mockk<() -> Unit>(relaxed = true)
        val stateWithNextPage = HistoryUiState(
            records = listOf(),
            hasNextPage = true,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            HistoryScreen(
                uiState = stateWithNextPage,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = mockOnLoadNextPage,
                    onSearchQueryChange = {}
                )
            )
        }

        composeTestRule.onNodeWithText("もっと見る").performClick()

        // Assert
        verify { mockOnLoadNextPage() }
    }
}
