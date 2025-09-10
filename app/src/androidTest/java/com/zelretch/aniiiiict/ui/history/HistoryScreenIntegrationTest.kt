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
import com.zelretch.aniiiiict.testing.HiltComposeTestRule
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
    val annictRepository: AnnictRepository = mockk<AnnictRepository>(relaxed = true)

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

    @Test
    fun historyScreen_再試行クリック_RepositoryのgetRecordsが呼ばれる() {
        // Arrange
        clearMocks(annictRepository)
        coEvery { annictRepository.getRecords(null) } returns PaginatedRecords(
            records = emptyList(),
            hasNextPage = false,
            endCursor = null
        )

        val viewModel = HistoryViewModel(loadRecordsUseCase, searchRecordsUseCase, deleteRecordUseCase)
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
                    onSearchQueryChange = {}
                )
            )
        }

        testRule.composeTestRule.onNodeWithText("再試行").performClick()

        // Assert
        coVerify(atLeast = 1) { annictRepository.getRecords(null) }
    }

    @Test
    fun historyScreen_削除クリック_RepositoryのdeleteRecordが呼ばれる() {
        // Arrange
        clearMocks(annictRepository)
        coEvery { annictRepository.deleteRecord(any()) } returns true

        val viewModel = HistoryViewModel(loadRecordsUseCase, searchRecordsUseCase, deleteRecordUseCase)
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
                    onSearchQueryChange = {}
                )
            )
        }

        testRule.composeTestRule.onNodeWithContentDescription("削除").performClick()

        // Assert
        coVerify(exactly = 1) { annictRepository.deleteRecord("r1") }
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

        // Clear any existing mocks to avoid conflicts
        clearMocks(annictRepository)

        // Set up mock to return hasNextPage = true for initial call and data for next page
        coEvery { annictRepository.getRecords(null) } returns PaginatedRecords(
            records = listOf(record),
            hasNextPage = true,
            endCursor = "cursor1"
        )
        coEvery { annictRepository.getRecords("cursor1") } returns PaginatedRecords(
            records = emptyList(),
            hasNextPage = false,
            endCursor = null
        )

        val viewModel = HistoryViewModel(loadRecordsUseCase, searchRecordsUseCase, deleteRecordUseCase)

        // Give the ViewModel a moment to start its initial loading from init block
        testRule.composeTestRule.waitForIdle()

        // Wait for initial ViewModel loading to complete
        testRule.composeTestRule.waitUntil(timeoutMillis = 5000) {
            !viewModel.uiState.value.isLoading
        }

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
                    onSearchQueryChange = {}
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

        // Verify that the ViewModel's loadNextPage method was called and completed
        // This ensures the integration between UI and ViewModel works correctly

        // Assert - Verify both the initial call from init and the next page call happened in correct order
        coVerifyOrder {
            annictRepository.getRecords(null) // 初期ロード時（init内で呼ばれる）
            annictRepository.getRecords("cursor1") // 次ページ読み込み時
        }
    }
}
