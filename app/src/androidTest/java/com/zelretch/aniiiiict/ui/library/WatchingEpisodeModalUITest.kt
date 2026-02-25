package com.zelretch.aniiiiict.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.SeasonName
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
 * WatchingEpisodeModalの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class WatchingEpisodeModalUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createMockViewModel(entry: LibraryEntry): WatchingEpisodeModalViewModel =
        mockk<WatchingEpisodeModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                WatchingEpisodeModalState(
                    episode = entry.nextEpisode,
                    selectedStatus = entry.work.viewerStatusState,
                    workId = entry.work.id,
                    workTitle = entry.work.title,
                    noEpisodes = entry.work.noEpisodes
                )
            )
        }

    private fun sampleLibraryEntry(
        status: StatusState = StatusState.WATCHING,
        noEpisodes: Boolean = false
    ): LibraryEntry {
        val work = Work(
            id = "work-1",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = status,
            noEpisodes = noEpisodes
        )
        val episode = if (noEpisodes) {
            null
        } else {
            Episode(
                id = "ep1",
                title = "第1話",
                numberText = "1",
                number = 1
            )
        }
        return LibraryEntry(
            id = "lib-entry-1",
            work = work,
            statusState = status,
            nextEpisode = episode
        )
    }

    @Test
    fun watchingEpisodeModal_基本要素_タイトルと閉じるボタンが表示される() {
        // Arrange
        val entry = sampleLibraryEntry()
        val viewModel = createMockViewModel(entry)
        val mockOnDismiss = mockk<() -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = mockOnDismiss,
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("テストアニメ").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("閉じる").assertIsDisplayed()
    }

    @Test
    fun watchingEpisodeModal_閉じるボタンクリック_onDismissが呼ばれる() {
        // Arrange
        val entry = sampleLibraryEntry()
        val viewModel = createMockViewModel(entry)
        val mockOnDismiss = mockk<() -> Unit>(relaxed = true)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
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
    fun watchingEpisodeModal_エピソード情報_正しく表示される() {
        // Arrange
        val entry = sampleLibraryEntry()
        val viewModel = createMockViewModel(entry)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("次のエピソード").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("第1話").assertIsDisplayed()
    }

    @Test
    fun watchingEpisodeModal_視聴済みボタン_クリックでrecordEpisode呼び出し() {
        // Arrange
        val entry = sampleLibraryEntry()
        val viewModel = createMockViewModel(entry)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("視聴済みにする").performClick()

        // Assert
        verify { viewModel.recordEpisode() }
    }

    @Test
    fun watchingEpisodeModal_エピソードなし_メッセージが表示される() {
        // Arrange
        val work = Work(
            id = "work-2",
            title = "エピソードなしアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING
        )
        val entryWithoutEpisode = LibraryEntry(
            id = "lib-entry-2",
            work = work,
            statusState = StatusState.WATCHING,
            nextEpisode = null
        )
        val viewModel = mockk<WatchingEpisodeModalViewModel>(relaxed = true).apply {
            every { state } returns MutableStateFlow(
                WatchingEpisodeModalState(
                    episode = null,
                    selectedStatus = StatusState.WATCHING,
                    workId = work.id,
                    workTitle = work.title
                )
            )
        }

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entryWithoutEpisode,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("次のエピソード情報がありません").assertIsDisplayed()
    }

    @Test
    fun watchingEpisodeModal_ステータスドロップダウン_展開して選択できる() {
        // Arrange
        val entry = sampleLibraryEntry(StatusState.WATCHING)
        val viewModel = createMockViewModel(entry)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()

        // Assert - 初期選択状態が表示されている
        composeTestRule.onNodeWithText("視聴中").assertIsDisplayed()

        // クリックでメニュー展開
        composeTestRule.onNodeWithText("視聴中").performClick()

        // Wait for dropdown to expand
        composeTestRule.waitForIdle()

        // いずれかの選択肢が表示される（見た を例に）
        composeTestRule.onNodeWithText("見た").assertIsDisplayed()

        // 選択できることを確認
        composeTestRule.onNodeWithText("見た").performClick()

        // Wait for selection to process
        composeTestRule.waitForIdle()

        // ViewModelのchangeStatusが呼ばれたことを確認
        verify { viewModel.changeStatus(StatusState.WATCHED) }
    }

    @Test
    fun watchingEpisodeModal_noEpisodesがtrue_エピソード記録UIが非表示になる() {
        // Arrange
        val entry = sampleLibraryEntry(noEpisodes = true)
        val viewModel = createMockViewModel(entry)

        // Act
        composeTestRule.setContent {
            WatchingEpisodeModal(
                entry = entry,
                onDismiss = {},
                viewModel = viewModel,
                onRefresh = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("この作品にはエピソード情報がありません。ステータスの変更のみ可能です。").assertIsDisplayed()
        composeTestRule.onNodeWithText("次のエピソード").assertDoesNotExist()
        composeTestRule.onNodeWithText("視聴済みにする").assertDoesNotExist()
    }
}
