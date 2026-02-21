package com.zelretch.aniiiiict.ui.history

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.DeleteRecordUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadRecordsUseCase
import com.zelretch.aniiiiict.domain.usecase.SearchRecordsUseCase
import com.zelretch.aniiiiict.testing.FakeAnnictRepository
import com.zelretch.aniiiiict.testing.HiltComposeTestRule
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.base.ErrorMapper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * HistoryScreenの統合テスト。
 * UI操作からViewModel、UseCaseを経由し、Repository（モック）が
 * 正しく呼び出されるかという、コンポーネント間の連携を検証する。
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class HistoryScreenIntegrationTest {

    @get:Rule
    val testRule = HiltComposeTestRule(this)

    @Inject
    lateinit var loadRecordsUseCase: LoadRecordsUseCase

    @Inject
    lateinit var searchRecordsUseCase: SearchRecordsUseCase

    @Inject
    lateinit var deleteRecordUseCase: DeleteRecordUseCase

    @BindValue
    @JvmField
    val annictRepository: AnnictRepository = FakeAnnictRepository()

    @BindValue
    @JvmField
    val aniListRepository: AniListRepository = mockk<AniListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val myAnimeListRepository: MyAnimeListRepository = mockk<MyAnimeListRepository>(relaxed = true)

    @BindValue
    @JvmField
    val programFilter: ProgramFilter = mockk<ProgramFilter>(relaxed = true)

    @BindValue
    @JvmField
    val customTabsIntentFactory: CustomTabsIntentFactory = mockk<CustomTabsIntentFactory>().apply {
        every { create() } returns mockk(relaxed = true)
    }

    private val fakeAnnictRepository get() = annictRepository as FakeAnnictRepository

    @Test
    fun historyScreen_再試行クリック_RepositoryのgetRecordsが呼ばれる() {
        // Arrange
        fakeAnnictRepository.getRecordsCalls.clear()
        fakeAnnictRepository.recordsResult = Result.success(
            PaginatedRecords(
                records = emptyList(),
                hasNextPage = false,
                endCursor = null
            )
        )

        val viewModel =
            HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                mockk<ErrorMapper>(relaxed = true)
            )
        val errorState = HistoryUiState(records = emptyList(), error = "エラーです")

        // Act
        testRule.composeTestRule.setContent {
            HistoryScreen(
                uiState = errorState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = { viewModel.loadRecords() },
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        testRule.composeTestRule.onNodeWithText("再試行").performClick()

        // Assert
        assertTrue(fakeAnnictRepository.getRecordsCalls.contains(null))
    }

    @Test
    fun historyScreen_削除クリック_RepositoryのdeleteRecordが呼ばれる() {
        // Arrange
        fakeAnnictRepository.deleteRecordCalls.clear()
        // deleteRecordResult defaults to Result.success(Unit)

        val viewModel =
            HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                mockk<ErrorMapper>(relaxed = true)
            )
        val work = Work(
            id = "w1",
            title = "Title",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHED
        )
        val ep = Episode(id = "e1", title = "第1話", numberText = "1", number = 1)
        val record = Record(
            id = "r1",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = ep,
            work = work
        )
        val ui = HistoryUiState(records = listOf(record))

        // Act
        testRule.composeTestRule.setContent {
            HistoryScreen(
                uiState = ui,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = { id -> viewModel.deleteRecord(id) },
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        testRule.composeTestRule.onNodeWithContentDescription("削除").performClick()

        // Assert
        assertTrue(fakeAnnictRepository.deleteRecordCalls.contains("r1"))
    }

    @Test
    fun historyScreen_次ページ読み込み_正しい順序でRepositoryが呼ばれる() = runBlocking {
        // Arrange - Create test data
        val work = Work(
            id = "w1",
            title = "Test Work",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHED
        )
        val ep = Episode(id = "e1", title = "第1話", numberText = "1", number = 1)
        val record = Record(
            id = "r1",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = ep,
            work = work
        )

        // Clear call tracking and set up first page result
        fakeAnnictRepository.getRecordsCalls.clear()
        fakeAnnictRepository.recordsResult = Result.success(
            PaginatedRecords(
                records = listOf(record),
                hasNextPage = true,
                endCursor = "cursor1"
            )
        )

        val viewModel =
            HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                mockk<ErrorMapper>(relaxed = true)
            )

        // Give the ViewModel a moment to start its initial loading from init block
        testRule.composeTestRule.waitForIdle()

        // Wait for initial ViewModel loading to complete
        testRule.composeTestRule.waitUntil(timeoutMillis = 5000) {
            !viewModel.uiState.value.isLoading
        }

        // Switch to second page result before triggering next page load
        fakeAnnictRepository.recordsResult = Result.success(
            PaginatedRecords(
                records = emptyList(),
                hasNextPage = false,
                endCursor = null
            )
        )

        // For integration testing, we need to ensure the UI state is correct
        // Create a controlled state that represents the expected state after ViewModel initialization
        val testState = HistoryUiState(
            records = listOf(record),
            allRecords = listOf(record),
            hasNextPage = true,
            isLoading = false,
            error = null,
            searchQuery = ""
        )

        // Act - Use the controlled state for reliable integration testing
        testRule.composeTestRule.setContent {
            HistoryScreen(
                uiState = testState,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = { viewModel.loadNextPage() },
                    onSearchQueryChange = {},
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // Wait for UI to render and be idle
        testRule.composeTestRule.waitForIdle()

        // Click the "もっと見る" button
        testRule.composeTestRule.onNodeWithText("もっと見る").performClick()

        // Wait for next page load to complete
        testRule.composeTestRule.waitUntil(timeoutMillis = 5000) {
            !viewModel.uiState.value.isLoading
        }

        // Assert - Verify both the initial call from init and the next page call happened in correct order
        val calls = fakeAnnictRepository.getRecordsCalls
        val nullIndex = calls.indexOf(null)
        val cursorIndex = calls.indexOf("cursor1")
        assertTrue("getRecords(null) should have been called", nullIndex >= 0)
        assertTrue("getRecords(\"cursor1\") should have been called", cursorIndex >= 0)
        assertTrue(
            "getRecords(null) should be called before getRecords(\"cursor1\")",
            nullIndex < cursorIndex
        )
    }

    @Test
    fun historyScreen_検索実行_検索クエリで表示が更新される() {
        // Arrange
        val work = Work(
            id = "w1",
            title = "Alpha Show",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHED
        )
        val ep = Episode(id = "e1", title = "第1話", numberText = "1", number = 1)
        val recordA = Record(
            id = "rA",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = ep,
            work = work.copy(title = "Alpha Show")
        )
        val recordB = Record(
            id = "rB",
            comment = null,
            rating = null,
            createdAt = ZonedDateTime.now(),
            episode = ep,
            work = work.copy(title = "Beta Show")
        )

        // 初期ロードで2件返す
        fakeAnnictRepository.recordsResult = Result.success(
            PaginatedRecords(
                records = listOf(recordA, recordB),
                hasNextPage = false,
                endCursor = null
            )
        )

        val viewModel =
            HistoryViewModel(
                loadRecordsUseCase,
                searchRecordsUseCase,
                deleteRecordUseCase,
                mockk<ErrorMapper>(relaxed = true)
            )

        // Wait for initial load
        testRule.composeTestRule.waitUntil(timeoutMillis = 5_000) { !viewModel.uiState.value.isLoading }

        // Use current vm state for UI
        val initialUi = viewModel.uiState.value

        // Act: render and then search for "Alpha"
        testRule.composeTestRule.setContent {
            HistoryScreen(
                uiState = initialUi,
                actions = HistoryScreenActions(
                    onNavigateBack = {},
                    onRetry = {},
                    onDeleteRecord = {},
                    onRefresh = {},
                    onLoadNextPage = {},
                    onSearchQueryChange = { q -> viewModel.updateSearchQuery(q) },
                    onRecordClick = {},
                    onDismissRecordDetail = {}
                )
            )
        }

        // 検索クエリを更新
        // 直接 ViewModel を呼んでから UI の確認対象（records 件数）が更新されるのを待つ
        testRule.composeTestRule.runOnIdle { viewModel.updateSearchQuery("Alpha") }
        testRule.composeTestRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.records.size == 1 }

        // Assert: フィルタされた件数が1件（Alphaのみ）
        assert(viewModel.uiState.value.records.first().work.title.contains("Alpha"))
    }
}
