package com.zelretch.aniiiiict.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkPriority
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LibraryScreenの純粋なCompose UIテスト。
 * ViewModelをモック化し、特定のUI状態が与えられた際の
 * UIの描画とインタラクションを検証する。
 */
@RunWith(AndroidJUnit4::class)
class LibraryScreenUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun libraryScreen_初期状態_基本要素が表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val initialState = LibraryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ライブラリ").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("戻る").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_エラー状態_エラーメッセージが表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val errorState = LibraryUiState(error = "ネットワークエラーが発生しました")
        every { mockViewModel.uiState } returns MutableStateFlow(errorState)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = errorState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("ネットワークエラーが発生しました").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_ローディング状態_ローディング表示が表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val loadingState = LibraryUiState(isLoading = true)
        every { mockViewModel.uiState } returns MutableStateFlow(loadingState)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = loadingState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("読み込み中...").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_同期中状態_更新中メッセージが表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val syncingState = LibraryUiState(isSyncing = true)
        every { mockViewModel.uiState } returns MutableStateFlow(syncingState)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = syncingState,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("更新中のためしばらくお待ちください").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_エントリーが存在する_カードが表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
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
        val stateWithEntries = LibraryUiState(
            entries = entries,
            allEntries = entries
        )
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithEntries)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = stateWithEntries,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("天国大魔境").assertIsDisplayed()
        composeTestRule.onNodeWithText("2023年 · TV").assertIsDisplayed()
        composeTestRule.onNodeWithText("次").assertIsDisplayed()
        composeTestRule.onNodeWithText("第9話「学園の子供たち」").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_フィルターが表示されている_検索バーが表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val stateWithFilter = LibraryUiState(isFilterVisible = true)
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithFilter)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = stateWithFilter,
                onNavigateBack = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("タイトル検索").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_フィルターボタンクリック_ViewModelメソッドが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val initialState = LibraryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
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
    fun libraryScreen_戻るボタンクリック_コールバックが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val initialState = LibraryUiState()
        every { mockViewModel.uiState } returns MutableStateFlow(initialState)
        var backPressed = false

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = initialState,
                onNavigateBack = { backPressed = true }
            )
        }
        composeTestRule.onNodeWithContentDescription("戻る").performClick()

        // Assert
        assert(backPressed)
    }

    @Test
    fun libraryScreen_ステータスフィルターチップクリック_ダイアログが開きtoggleStatusFilterが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val stateWithFilter = LibraryUiState(
            isFilterVisible = true,
            availableStatuses = listOf(StatusState.WATCHING, StatusState.WANNA_WATCH)
        )
        every { mockViewModel.uiState } returns MutableStateFlow(stateWithFilter)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = stateWithFilter,
                onNavigateBack = {}
            )
        }
        composeTestRule.onNodeWithText("ステータス").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("見てる").performClick()

        // Assert
        verify { mockViewModel.toggleStatusFilter(StatusState.WATCHING) }
    }

    @Test
    fun libraryScreen_見たボタンクリック_recordNextEpisodeが呼ばれる() {
        // Arrange - nextEpisode を持つ視聴中エントリー
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = LibraryEntry(
            id = "entry1",
            work = Work(id = "work1", title = "テストアニメ", viewerStatusState = StatusState.WATCHING),
            nextEpisode = Episode(id = "ep1", title = "始まり", numberText = "1", number = 1),
            statusState = StatusState.WATCHING
        )
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = state,
                onNavigateBack = {}
            )
        }

        // Assert - ステータスチップ（見てる）と「見た」ボタンが表示される
        composeTestRule.onNodeWithText("見てる").assertIsDisplayed()
        composeTestRule.onNodeWithText("見た").assertIsDisplayed()

        // 「見た」ボタンをクリックすると recordNextEpisode が呼ばれる
        composeTestRule.onNodeWithText("見た").performClick()
        verify { mockViewModel.recordNextEpisode(entry) }
    }

    @Test
    fun libraryScreen_視聴済みエントリー_すべて視聴済みが表示される() {
        // Arrange - nextEpisode 無し
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = LibraryEntry(
            id = "entry1",
            work = Work(id = "work1", title = "テストアニメ", viewerStatusState = StatusState.WATCHED),
            nextEpisode = null,
            statusState = StatusState.WATCHED
        )
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = state,
                onNavigateBack = {}
            )
        }

        // Assert - 「次/見た」ではなく「すべて視聴済み」が出る
        composeTestRule.onNodeWithText("すべて視聴済み").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_カードタップ_作品詳細へ遷移する() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = LibraryEntry(
            id = "entry1",
            work = Work(id = "work1", title = "テストアニメ", viewerStatusState = StatusState.WATCHING),
            nextEpisode = Episode(id = "ep1", numberText = "1", number = 1),
            statusState = StatusState.WATCHING
        )
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)
        var navigatedWorkId: String? = null

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = state,
                onNavigateBack = {},
                onNavigateToDetail = { navigatedWorkId = it }
            )
        }
        // カード本体（タイトル）をタップ
        composeTestRule.onNodeWithText("テストアニメ").performClick()

        // Assert - インライン展開やモーダルではなく作品詳細へ遷移
        assert(navigatedWorkId == "work1")
    }

    @Test
    fun libraryScreen_エピソード情報がない作品_すべて視聴済みと表示されない() {
        // Arrange - 映画等、Annict上にエピソード情報が無く未視聴（見たい）のケース
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = LibraryEntry(
            id = "entry1",
            work = Work(
                id = "work1",
                title = "テスト映画",
                viewerStatusState = StatusState.WANNA_WATCH,
                noEpisodes = true
            ),
            nextEpisode = null,
            statusState = StatusState.WANNA_WATCH
        )
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(
                viewModel = mockViewModel,
                uiState = state,
                onNavigateBack = {}
            )
        }

        // Assert - 未視聴なのに「すべて視聴済み」と表示されてはいけない
        composeTestRule.onNodeWithText("すべて視聴済み").assertDoesNotExist()
        composeTestRule.onNodeWithText("エピソード情報なし").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_まとめてボタンクリック_toggleBulkRecordが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = bulkEntry()
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }
        composeTestRule.onNodeWithContentDescription("まとめて記録").performClick()

        // Assert
        verify { mockViewModel.toggleBulkRecord(entry) }
    }

    @Test
    fun libraryScreen_まとめて展開中_未視聴一覧が表示されタップでbulkRecordUpToが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = bulkEntry()
        val state = LibraryUiState(
            entries = listOf(entry),
            allEntries = listOf(entry),
            bulkRecordEntryId = entry.id,
            bulkEpisodes = BulkEpisodesState.Loaded(
                listOf(
                    Episode(id = "ep2", numberText = "第2話", number = 2, title = "二話"),
                    Episode(id = "ep3", numberText = "第3話", number = 3, title = "三話")
                )
            )
        )
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }

        // Assert
        composeTestRule.onNodeWithText("未視聴 2話 ・ タップで記録").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inline_episode_1").performClick()
        verify { mockViewModel.bulkRecordUpTo(entry, 1) }
    }

    @Test
    fun libraryScreen_まとめて展開中_未視聴が無い場合はその旨が表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = bulkEntry()
        val state = LibraryUiState(
            entries = listOf(entry),
            allEntries = listOf(entry),
            bulkRecordEntryId = entry.id,
            bulkEpisodes = BulkEpisodesState.Loaded(emptyList())
        )
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }

        // Assert
        composeTestRule.onNodeWithText("未視聴のエピソードはありません").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_最終話確認中_ダイアログが表示されボタンでconfirmとdismissが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = bulkEntry()
        val state = LibraryUiState(
            entries = listOf(entry),
            allEntries = listOf(entry),
            finaleConfirmation = FinaleConfirmation(entryId = entry.id, workId = entry.work.id, episodeNumber = 12)
        )
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }

        // Assert
        composeTestRule.onNodeWithText("最終話確認").assertIsDisplayed()
        composeTestRule.onNodeWithText("視聴完了にする").performClick()
        verify { mockViewModel.confirmFinale() }
        composeTestRule.onNodeWithText("後で").performClick()
        verify { mockViewModel.dismissFinale() }
    }

    @Test
    fun libraryScreen_カード長押し_優先度ダイアログからsetPriorityが呼ばれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val entry = bulkEntry()
        val state = LibraryUiState(entries = listOf(entry), allEntries = listOf(entry))
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }
        composeTestRule.onNodeWithText("テストアニメ").performTouchInput { longClick() }

        // Assert
        composeTestRule.onNodeWithText("優先度（この端末だけに保存されます）").assertIsDisplayed()
        composeTestRule.onNodeWithTag("priority_option_FEATURED").performClick()
        verify { mockViewModel.setPriority(entry, WorkPriority.FEATURED) }
    }

    @Test
    fun libraryScreen_優先度あり_セクション見出しが出て後回しは折りたたまれる() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val featured = priorityEntry("w1", "注目アニメ", WorkPriority.FEATURED)
        val normal = priorityEntry("w2", "ふつうアニメ", WorkPriority.NORMAL)
        val deferred = priorityEntry("w3", "後回しアニメ", WorkPriority.DEFERRED)
        val entries = listOf(featured, normal, deferred)
        val state = LibraryUiState(entries = entries, allEntries = entries)
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }

        // Assert
        composeTestRule.onNodeWithText("注目（1）").assertIsDisplayed()
        composeTestRule.onNodeWithText("ふつう（1）").assertIsDisplayed()
        composeTestRule.onNodeWithText("後回し（1）").assertIsDisplayed()
        composeTestRule.onNodeWithText("注目アニメ").assertIsDisplayed()
        composeTestRule.onNodeWithText("後回しアニメ").assertDoesNotExist()
        composeTestRule.onNodeWithTag("library_section_DEFERRED").performClick()
        verify { mockViewModel.toggleDeferredSection() }
    }

    @Test
    fun libraryScreen_後回し展開中_後回しの作品が表示される() {
        // Arrange
        val mockViewModel = mockk<LibraryViewModel>(relaxed = true)
        val deferred = priorityEntry("w3", "後回しアニメ", WorkPriority.DEFERRED)
        val state = LibraryUiState(entries = listOf(deferred), allEntries = listOf(deferred), isDeferredExpanded = true)
        every { mockViewModel.uiState } returns MutableStateFlow(state)

        // Act
        composeTestRule.setContent {
            LibraryScreen(viewModel = mockViewModel, uiState = state, onNavigateBack = {})
        }

        // Assert
        composeTestRule.onNodeWithText("後回しアニメ").assertIsDisplayed()
    }

    private fun priorityEntry(id: String, title: String, priority: WorkPriority) = LibraryEntry(
        id = "entry_$id",
        work = Work(id = id, title = title, viewerStatusState = StatusState.WATCHING),
        nextEpisode = null,
        statusState = StatusState.WATCHING,
        priority = priority
    )

    private fun bulkEntry() = LibraryEntry(
        id = "entry1",
        work = Work(id = "work1", title = "テストアニメ", viewerStatusState = StatusState.WATCHING),
        nextEpisode = Episode(id = "ep2", title = "二話", numberText = "第2話", number = 2),
        statusState = StatusState.WATCHING
    )
}
