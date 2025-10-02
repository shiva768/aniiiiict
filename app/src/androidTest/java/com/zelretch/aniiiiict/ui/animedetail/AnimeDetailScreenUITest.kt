package com.zelretch.aniiiiict.ui.animedetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.annict.WorkDetailQuery
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * UI test for AnimeDetailScreen.
 * Following AGENTS.md strategy:
 * - Mock the ViewModel
 * - Verify consistency between UIState and screen's state
 * - Do not verify internal method calls
 * - Focus on end-user visible behavior
 */
class AnimeDetailScreenUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 初期状態でローディング表示される() {
        // Given: ローディング中のUIState
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(isLoading = true)
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: ローディングインジケータが表示される
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun ロード完了後に基本情報が表示される() {
        // Given: アニメ詳細情報を含むUIState
        val animeDetailInfo = createAnimeDetailInfo(
            episodeCount = 24,
            imageUrl = "https://example.com/image.jpg"
        )
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                animeDetailInfo = animeDetailInfo
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 基本情報が表示される
        composeTestRule.onNodeWithText("全24話").assertIsDisplayed()
        composeTestRule.onNodeWithText("基本情報").assertIsDisplayed()
    }

    @Test
    fun エラー発生時にエラーメッセージが表示される() {
        // Given: エラー状態のUIState
        val errorMessage = "アニメ詳細情報の取得に失敗しました"
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                error = errorMessage
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: エラーメッセージが表示される
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun MyAnimeList情報がある場合に放送状況と種別が表示される() {
        // Given: MyAnimeList情報を含むAnimeDetailInfo
        val malInfo = MyAnimeListResponse(
            id = 12345,
            mediaType = "tv",
            numEpisodes = 24,
            status = "finished_airing",
            broadcast = null
        )
        val animeDetailInfo = createAnimeDetailInfo(
            malInfo = malInfo
        )
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                animeDetailInfo = animeDetailInfo
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: MAL情報が表示される
        composeTestRule.onNodeWithText("放送状況").assertIsDisplayed()
        composeTestRule.onNodeWithText("finished_airing").assertIsDisplayed()
        composeTestRule.onNodeWithText("種別").assertIsDisplayed()
        composeTestRule.onNodeWithText("tv").assertIsDisplayed()
    }

    @Test
    fun 外部リンクがある場合に公式サイトとWikipediaが表示される() {
        // Given: 外部リンクを含むAnimeDetailInfo
        val animeDetailInfo = createAnimeDetailInfo(
            officialSiteUrl = "https://example.com/official",
            wikipediaUrl = "https://ja.wikipedia.org/wiki/テストアニメ"
        )
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                animeDetailInfo = animeDetailInfo
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 外部リンクセクションが表示される
        composeTestRule.onNodeWithText("外部リンク").assertIsDisplayed()
        composeTestRule.onNodeWithText("公式サイト").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wikipedia").assertIsDisplayed()
    }

    @Test
    fun 配信プラットフォーム情報がある場合にセクションが表示される() {
        // Given: 配信プラットフォーム情報を含むAnimeDetailInfo
        val mockChannel = mockk<WorkDetailQuery.Channel>()
        every { mockChannel.id } returns "channel-1"
        every { mockChannel.name } returns "テストチャンネル"

        val mockProgram = mockk<WorkDetailQuery.Node1>()
        every { mockProgram.id } returns "program-1"
        every { mockProgram.startedAt } returns "2024-01-01T12:00:00Z"
        every { mockProgram.channel } returns mockChannel

        val animeDetailInfo = createAnimeDetailInfo(
            programs = listOf(mockProgram)
        )
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                animeDetailInfo = animeDetailInfo
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 配信プラットフォームセクションが表示される
        composeTestRule.onNodeWithText("配信プラットフォーム").assertIsDisplayed()
        composeTestRule.onNodeWithText("テストチャンネル").assertIsDisplayed()
    }

    @Test
    fun 関連作品情報がある場合にセクションが表示される() {
        // Given: 関連作品情報を含むAnimeDetailInfo
        val mockRelatedWork = mockk<WorkDetailQuery.Node3>()
        every { mockRelatedWork.id } returns "work-1"
        every { mockRelatedWork.title } returns "関連作品タイトル"
        every { mockRelatedWork.titleEn } returns null
        every { mockRelatedWork.seasonName } returns null
        every { mockRelatedWork.seasonYear } returns null
        every { mockRelatedWork.image } returns null

        val mockWorks = mockk<WorkDetailQuery.Works>()
        every { mockWorks.nodes } returns listOf(mockRelatedWork)

        val mockSeries = mockk<WorkDetailQuery.Node2>()
        every { mockSeries.id } returns "series-1"
        every { mockSeries.name } returns "テストシリーズ"
        every { mockSeries.nameEn } returns "Test Series"
        every { mockSeries.works } returns mockWorks

        val animeDetailInfo = createAnimeDetailInfo(
            seriesList = listOf(mockSeries)
        )
        val mockViewModel = createMockViewModel(
            state = AnimeDetailState(
                isLoading = false,
                animeDetailInfo = animeDetailInfo
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 関連作品セクションが表示される
        composeTestRule.onNodeWithText("関連作品").assertIsDisplayed()
        composeTestRule.onNodeWithText("テストシリーズ").assertIsDisplayed()
        // 関連作品は "  • タイトル" の形式で表示される
        composeTestRule.onNodeWithText("  • 関連作品タイトル").assertIsDisplayed()
    }

    // Helper functions

    private fun createMockViewModel(state: AnimeDetailState): AnimeDetailViewModel = mockk<AnimeDetailViewModel> {
        coEvery { this@mockk.state } returns MutableStateFlow(state)
        coEvery { loadAnimeDetail(any()) } returns Unit
    }

    private fun createSampleProgramWithWork(): ProgramWithWork {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        val episode = Episode(
            id = "episode-id",
            number = 1,
            numberText = "1",
            title = "第1話"
        )

        val channel = Channel(name = "テストチャンネル")

        val program = Program(
            id = "program-id",
            startedAt = LocalDateTime.now(),
            channel = channel,
            episode = episode
        )

        return ProgramWithWork(
            work = work,
            programs = listOf(program),
            firstProgram = program
        )
    }

    private fun createAnimeDetailInfo(
        episodeCount: Int? = null,
        imageUrl: String? = null,
        officialSiteUrl: String? = null,
        wikipediaUrl: String? = null,
        malInfo: MyAnimeListResponse? = null,
        programs: List<WorkDetailQuery.Node1?>? = null,
        seriesList: List<WorkDetailQuery.Node2?>? = null
    ): AnimeDetailInfo {
        val work = Work(
            id = "test-work-id",
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = "12345",
            viewerStatusState = StatusState.WATCHING
        )

        return AnimeDetailInfo(
            work = work,
            programs = programs,
            seriesList = seriesList,
            malInfo = malInfo,
            episodeCount = episodeCount,
            imageUrl = imageUrl,
            officialSiteUrl = officialSiteUrl,
            wikipediaUrl = wikipediaUrl
        )
    }
}
