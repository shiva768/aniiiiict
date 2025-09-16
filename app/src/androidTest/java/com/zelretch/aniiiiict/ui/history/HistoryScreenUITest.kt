package com.zelretch.aniiiiict.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

/**
 * HistoryScreenの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class 履歴画面UIテスト {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun 履歴画面で初期状態で基本要素が表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val initialState = HistoryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        val mockOnNavigateBack = mockk<() -> Unit>(relaxed = true)
        val mockOnRetry = mockk<() -> Unit>(relaxed = true)
        val mockOnDeleteRecord = mockk<(String) -> Unit>(relaxed = true)
        val mockOnRefresh = mockk<() -> Unit>(relaxed = true)
        val mockOnLoadNextPage = mockk<() -> Unit>(relaxed = true)
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)
        val mockOnRecordClick = mockk<(Record) -> Unit>(relaxed = true)
        val mockOnDismissRecordDetail = mockk<() -> Unit>(relaxed = true)

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
                    onSearchQueryChange = mockOnSearchQueryChange,
                    onRecordClick = mockOnRecordClick,
                    onDismissRecordDetail = mockOnDismissRecordDetail
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴履歴").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("戻る").assertIsDisplayed()
        composeTestRule.onNodeWithText("作品名・エピソード名で検索").assertIsDisplayed()
    }

    @Test
    fun 履歴画面で空の状態で適切なメッセージが表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val emptyState = HistoryUiState(
            records = emptyList(),
            isLoading = false
        )
        every { mockViewModel.uiState } returns MutableStateFlow(emptyState)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("視聴履歴がありません").assertIsDisplayed()
        composeTestRule.onNodeWithText("下にスワイプして更新").assertIsDisplayed()
    }

    @Test
    fun 履歴画面で履歴データでレコードが表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
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
            title = "始まりの物語",
            numberText = "EP1",
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
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithRecords)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("テストアニメ").assertIsDisplayed()
        composeTestRule.onNodeWithText("EP1 始まりの物語").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("削除").assertIsDisplayed()
    }

    @Test
    fun 履歴画面で戻るボタンをクリックするとナビゲーションコールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnNavigateBack = mockk<() -> Unit>(relaxed = true)
        val initialState = HistoryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("戻る").performClick()

        // Assert
        verify { mockOnNavigateBack() }
    }

    @Test
    fun 履歴画面で検索入力するとコールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)
        val initialState = HistoryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

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
                    onSearchQueryChange = mockOnSearchQueryChange,
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithText("作品名・エピソード名で検索").performTextInput("テスト")

        // Assert
        verify { mockOnSearchQueryChange("テスト") }
    }

    @Test
    fun 履歴画面で検索文字入力済みでクリアボタンが表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val stateWithSearchQuery = HistoryUiState(searchQuery = "テストクエリ")
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithSearchQuery)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithContentDescription("クリア").assertIsDisplayed()
    }

    @Test
    fun 履歴画面でクリアボタンをクリックすると検索文字がクリアされる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnSearchQueryChange = mockk<(String) -> Unit>(relaxed = true)
        val stateWithSearchQuery = HistoryUiState(searchQuery = "テストクエリ")
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithSearchQuery)

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
                    onSearchQueryChange = mockOnSearchQueryChange,
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("クリア").performClick()

        // Assert
        verify { mockOnSearchQueryChange("") }
    }

    @Test
    fun 履歴画面で削除ボタンをクリックすると削除コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
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
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithRecords)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("削除").performClick()

        // Assert
        verify { mockOnDeleteRecord("record1") }
    }

    @Test
    fun 履歴画面でエラー状態でエラーメッセージと再試行ボタンが表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val errorState = HistoryUiState(
            records = emptyList(),
            error = "ネットワークエラーが発生しました",
            isLoading = false
        )
        every { mockViewModel.uiState } returns MutableStateFlow(errorState)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ネットワークエラーが発生しました").assertIsDisplayed()
        composeTestRule.onNodeWithText("再試行").assertIsDisplayed()
    }

    @Test
    fun 履歴画面で再試行ボタンをクリックすると再試行コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnRetry = mockk<() -> Unit>(relaxed = true)
        val errorState = HistoryUiState(
            records = emptyList(),
            error = "エラーです",
            isLoading = false
        )
        every { mockViewModel.uiState } returns MutableStateFlow(errorState)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithText("再試行").performClick()

        // Assert
        verify { mockOnRetry() }
    }

    @Test
    fun 履歴画面で次のページありでもっと見るボタンが表示される() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val stateWithNextPage = HistoryUiState(
            records = listOf(), // レコードは空でも可
            hasNextPage = true,
            isLoading = false
        )
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithNextPage)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Assert
        composeTestRule.onNodeWithText("もっと見る").assertIsDisplayed()
    }

    @Test
    fun 履歴画面でもっと見るボタンをクリックすると次ページ読み込みコールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnLoadNextPage = mockk<() -> Unit>(relaxed = true)
        val stateWithNextPage = HistoryUiState(
            records = listOf(),
            hasNextPage = true,
            isLoading = false
        )
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithNextPage)

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
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        composeTestRule.onNodeWithText("もっと見る").performClick()

        // Assert
        verify { mockOnLoadNextPage() }
    }

    @Test
    fun 履歴画面でレコードカードをクリックすると詳細表示コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<HistoryViewModel>(relaxed = true)
        val mockOnRecordClick = mockk<(Record) -> Unit>(relaxed = true)

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
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithRecords)

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
                    onSearchQueryChange = {},
                    onRecordClick = mockOnRecordClick,
                    onDismissRecordDetail = {}
                )
            )
        }

        // Click on the record title (part of the clickable card)
        composeTestRule.onNodeWithText("テストアニメ").performClick()

        // Assert
        verify { mockOnRecordClick(sampleRecord) }
    }
}
