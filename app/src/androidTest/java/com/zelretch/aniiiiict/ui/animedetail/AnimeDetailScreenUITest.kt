package com.zelretch.aniiiiict.ui.animedetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.annict.WorkDetailQuery
import com.annict.WorkSeriesListQuery
import com.annict.type.Media
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.AnimeDetailInfo
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.ui.base.UiState
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
            state = UiState.Loading
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
    fun エピソード数が不明な場合に話数不明と表示される() {
        // Given: エピソード数がnullのUIState
        val animeDetailInfo = createAnimeDetailInfo(
            episodeCount = null,
            imageUrl = "https://example.com/image.jpg"
        )
        val mockViewModel = createMockViewModel(
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 話数不明が表示される
        composeTestRule.onNodeWithText("話数不明").assertIsDisplayed()
    }

    @Test
    fun エラー発生時にエラーメッセージが表示される() {
        // Given: エラー状態のUIState
        val errorMessage = "アニメ詳細情報の取得に失敗しました"
        val mockViewModel = createMockViewModel(
            state = UiState.Error(errorMessage)
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
            broadcast = null,
            mainPicture = null
        )
        val animeDetailInfo = createAnimeDetailInfo(
            malInfo = malInfo
        )
        val mockViewModel = createMockViewModel(
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
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
    fun キャスト情報がある場合にセクションが表示される() {
        // Given: キャスト（声優）情報を含むAnimeDetailInfo
        val mockCharacter = mockk<WorkDetailQuery.Character>()
        every { mockCharacter.name } returns "テストキャラ"
        val mockPerson = mockk<WorkDetailQuery.Person>()
        every { mockPerson.name } returns "テスト声優"
        val mockCast = mockk<WorkDetailQuery.Node2>()
        every { mockCast.id } returns "cast-1"
        every { mockCast.name } returns "テスト声優"
        every { mockCast.character } returns mockCharacter
        every { mockCast.person } returns mockPerson

        val animeDetailInfo = createAnimeDetailInfo(
            casts = listOf(mockCast)
        )
        val mockViewModel = createMockViewModel(
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: キャストセクションが表示される
        composeTestRule.onNodeWithText("キャスト").assertIsDisplayed()
        composeTestRule.onNodeWithText("テストキャラ").assertIsDisplayed()
        composeTestRule.onNodeWithText("テスト声優").assertIsDisplayed()
    }

    @Test
    fun 関連作品情報がある場合にセクションが表示される() {
        // Given: 関連作品情報を含むAnimeDetailInfo
        val mockSeries = mockSeriesNode(workId = "work-1", workTitle = "関連作品タイトル")

        val animeDetailInfo = createAnimeDetailInfo(
            seriesList = listOf(mockSeries)
        )
        val mockViewModel = createMockViewModel(
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: 関連作品セクションが表示される
        composeTestRule.onNodeWithText("関連作品").assertIsDisplayed()
        composeTestRule.onNodeWithText("テストシリーズ").assertIsDisplayed()
        composeTestRule.onNodeWithText("関連作品タイトル").assertIsDisplayed()
        // メディア種別バッジ（TV）が表示される
        composeTestRule.onNodeWithText("TV").assertIsDisplayed()
    }

    @Test
    fun 関連作品をタップすると遷移コールバックが呼ばれる() {
        // Given: 関連作品情報を含むAnimeDetailInfo
        val mockSeries = mockSeriesNode(workId = "related-work-id", workTitle = "関連作品タイトル")

        val animeDetailInfo = createAnimeDetailInfo(seriesList = listOf(mockSeries))
        val mockViewModel = createMockViewModel(
            state = UiState.Success(AnimeDetailData(animeDetailInfo = animeDetailInfo))
        )
        val programWithWork = createSampleProgramWithWork()
        var navigatedWorkId: String? = null

        // When: 画面を表示してタップ
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    onNavigateToWork = { navigatedWorkId = it },
                    viewModel = mockViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("関連作品タイトル").performClick()

        // Then: 関連作品IDでナビゲーションが呼ばれる
        assert(navigatedWorkId == "related-work-id")
    }

    @Test
    fun 成功時にステータスFilterChipが横並び表示される() {
        // Given: アニメ詳細情報とステータスを含むUIState
        val animeDetailInfo = createAnimeDetailInfo()
        val mockViewModel = createMockViewModel(
            state = UiState.Success(
                AnimeDetailData(
                    animeDetailInfo = animeDetailInfo,
                    selectedStatus = StatusState.WATCHING
                )
            )
        )
        val programWithWork = createSampleProgramWithWork()

        // When: 画面を表示
        composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    workId = "test-work-id",
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Then: ドロップダウンではなく、色付きFilterChipが全ステータス分横並び表示される
        composeTestRule.onNodeWithText("見てる").assertIsDisplayed()
        composeTestRule.onNodeWithText("見たい").assertIsDisplayed()
        composeTestRule.onNodeWithText("見た").assertIsDisplayed()
        composeTestRule.onNodeWithText("中断").assertIsDisplayed()
        composeTestRule.onNodeWithText("中止").assertIsDisplayed()
    }

    // Helper functions

    private fun createMockViewModel(state: UiState<AnimeDetailData>): AnimeDetailViewModel =
        mockk<AnimeDetailViewModel> {
            coEvery { this@mockk.uiState } returns MutableStateFlow(state)
            coEvery { loadAnimeDetail(any()) } returns Unit
            coEvery { loadAnimeDetailById(any()) } returns Unit
            coEvery { changeStatus(any()) } returns Unit
        }

    // Annict の Series.works は edges { item } 経由で取得するため、その構造でモックする
    private fun mockSeriesNode(workId: String, workTitle: String): WorkSeriesListQuery.Node1 {
        val mockItem = mockk<WorkSeriesListQuery.Item>()
        every { mockItem.id } returns workId
        every { mockItem.title } returns workTitle
        every { mockItem.media } returns Media.TV
        every { mockItem.seasonName } returns null
        every { mockItem.seasonYear } returns null
        every { mockItem.image } returns null

        val mockEdge = mockk<WorkSeriesListQuery.Edge>()
        every { mockEdge.item } returns mockItem

        val mockWorks = mockk<WorkSeriesListQuery.Works>()
        every { mockWorks.edges } returns listOf(mockEdge)

        val mockSeries = mockk<WorkSeriesListQuery.Node1>()
        every { mockSeries.id } returns "series-1"
        every { mockSeries.name } returns "テストシリーズ"
        every { mockSeries.works } returns mockWorks
        return mockSeries
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
            programs = listOf(program)
        )
    }

    private fun createAnimeDetailInfo(
        episodeCount: Int? = null,
        imageUrl: String? = null,
        officialSiteUrl: String? = null,
        wikipediaUrl: String? = null,
        malInfo: MyAnimeListResponse? = null,
        programs: List<WorkDetailQuery.Node1?>? = null,
        seriesList: List<WorkSeriesListQuery.Node1?>? = null,
        casts: List<WorkDetailQuery.Node2?>? = null
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
            casts = casts,
            seriesList = seriesList,
            malInfo = malInfo,
            episodeCount = episodeCount,
            imageUrl = imageUrl,
            officialSiteUrl = officialSiteUrl,
            wikipediaUrl = wikipediaUrl
        )
    }
}
