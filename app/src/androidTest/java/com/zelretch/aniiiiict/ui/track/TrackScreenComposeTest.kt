package com.zelretch.aniiiiict.ui.track

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.annict.type.Media
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.datastore.FilterPreferences
import com.zelretch.aniiiiict.data.model.AniListMedia
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.domain.filter.FilterState
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.FilterProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.FinaleState
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleResult
import com.zelretch.aniiiiict.domain.usecase.JudgeFinaleUseCase
import com.zelretch.aniiiiict.domain.usecase.LoadProgramsUseCase
import com.zelretch.aniiiiict.domain.usecase.UpdateViewStateUseCase
import com.zelretch.aniiiiict.domain.usecase.WatchEpisodeUseCase
import com.zelretch.aniiiiict.ui.common.LocalTestMode
import com.zelretch.aniiiiict.util.DisableAnimationsRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * TrackScreenのCompose UIテスト
 * 主要な画面要素の表示とユーザーインタラクションを検証
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TrackScreenComposeTest {

    private val composeTestRule = createComposeRule()
    private val disableAnimationsRule = DisableAnimationsRule()

    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(disableAnimationsRule)
        .around(composeTestRule)

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var annictRepository: AnnictRepository
    private lateinit var aniListRepository: AniListRepository
    private lateinit var filterPreferences: FilterPreferences
    private lateinit var judgeFinaleUseCase: JudgeFinaleUseCase
    private lateinit var viewModel: TrackViewModel

    private val filterStateFlow = MutableStateFlow(FilterState())

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        annictRepository = mockk<AnnictRepository>()
        aniListRepository = mockk<AniListRepository>()
        filterPreferences = mockk<FilterPreferences>()
        judgeFinaleUseCase = mockk<JudgeFinaleUseCase>()

        every { filterPreferences.filterState } returns filterStateFlow
        coEvery { filterPreferences.updateFilterState(any()) } returns Unit
        coEvery { annictRepository.getRawProgramsData() } returns flowOf(emptyList())
        coEvery { annictRepository.createRecord(any(), any()) } returns true
        coEvery { annictRepository.updateWorkViewStatus(any(), any()) } returns true
        coEvery { aniListRepository.getMedia(any()) } returns Result.success(
            AniListMedia(
                id = 1,
                episodes = 12,
                format = "TV",
                status = "RELEASING",
                nextAiringEpisode = NextAiringEpisode(episode = 2, airingAt = 0)
            )
        )

        val loadProgramsUseCase = LoadProgramsUseCase(annictRepository)
        val updateViewStateUseCase = UpdateViewStateUseCase(annictRepository)
        val watchEpisodeUseCase = WatchEpisodeUseCase(annictRepository, updateViewStateUseCase)
        val programFilter = ProgramFilter()
        val filterProgramsUseCase = FilterProgramsUseCase(programFilter)

        viewModel = TrackViewModel(
            loadProgramsUseCase,
            watchEpisodeUseCase,
            updateViewStateUseCase,
            filterProgramsUseCase,
            filterPreferences,
            judgeFinaleUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleProgramWithWork(
        workId: String = "1",
        title: String = "テストアニメ",
        episodeCount: Int = 1,
        isFinale: Boolean = false
    ): ProgramWithWork {
        val sampleWork = Work(
            id = workId,
            title = title,
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "TV",
            mediaText = "TV",
            viewerStatusState = StatusState.WATCHING,
            malAnimeId = "mal-123"
        )
        val sampleEpisodes = (1..episodeCount).map {
            Episode(
                id = "ep$it",
                title = "第${it}話",
                numberText = "$it",
                number = it
            )
        }
        val samplePrograms = sampleEpisodes.map {
            Program(
                id = "prog${it.number}",
                startedAt = LocalDateTime.now(),
                channel = Channel(name = "テストチャンネル"),
                episode = it
            )
        }
        coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
            FinaleState.NOT_FINALE,
            isFinale
        )
        return ProgramWithWork(
            programs = samplePrograms,
            firstProgram = samplePrograms.first(),
            work = sampleWork
        )
    }

    @Test
    fun trackScreen_初期状態_基本要素が表示される() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }

        composeTestRule.onNodeWithText("番組一覧").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("フィルター").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("履歴").assertIsDisplayed()
    }

    @Test
    fun trackScreen_ロード成功_番組リストが表示される() {
        sampleProgramWithWork(title = "ロード成功テスト")
        coEvery { annictRepository.getRawProgramsData() } returns flowOf(
            listOf(
                mockk {
                    every { id } returns "prog-id-1"
                    every { startedAt } returns "2025-01-01T12:00:00Z"
                    every { channel } returns mockk {
                        every { name } returns "テレビ東京"
                    }
                    every { episode } returns mockk {
                        every { id } returns "ep-id"
                        every { number } returns 1
                        every { numberText } returns "#1"
                        every { title } returns "エピソードタイトル"
                    }
                    every { work } returns mockk {
                        every { id } returns "work-1"
                        every { title } returns "ロード成功テスト"
                        every { seasonName } returns SeasonName.WINTER
                        every { seasonYear } returns 2025
                        every { media } returns Media.TV
                        every { malAnimeId } returns "456"
                        every { viewerStatusState } returns StatusState.WATCHING
                        every { image } returns mockk {
                            every { recommendedImageUrl } returns "https://example.com/image.jpg"
                            every { facebookOgImageUrl } returns "https://example.com/og_image.jpg"
                        }
                    }
                }
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }

        // Act
        viewModel.refresh()
        composeTestRule.mainClock.advanceTimeBy(3000) // Wait for debounce and loading

        // Assert
        composeTestRule.onNodeWithText("ロード成功テスト").assertIsDisplayed()
    }

    @Test
    fun trackScreen_ロード失敗_エラーが表示される() {
        coEvery { annictRepository.getRawProgramsData() } throws RuntimeException("Network Error")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }

        viewModel.refresh()
        composeTestRule.mainClock.advanceTimeBy(3000)

        composeTestRule.onNodeWithTag("snackbar").assertIsDisplayed()
        composeTestRule.onNodeWithText("処理中にエラーが発生しました").assertIsDisplayed()
    }

    @Test
    fun trackScreen_記録成功_スナックバーが表示される() {
        // Arrange
        val program = sampleProgramWithWork(workId = "work-rec", title = "記録成功テスト")
        coEvery { annictRepository.getRawProgramsData() } returns flowOf(createMockProgramData(program))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }
        viewModel.refresh()
        composeTestRule.mainClock.advanceTimeBy(3000)

        // Act
        composeTestRule.onNodeWithContentDescription("record-ep1").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000)

        // Assert
        composeTestRule.onNodeWithTag("snackbar").assertIsDisplayed()
        composeTestRule.onNodeWithText("「第1話」を記録しました").assertIsDisplayed()
        coVerify { annictRepository.createRecord("ep1", "work-rec") }
    }

    @Test
    fun trackScreen_フィナーレ確認_はいボタンで視聴済みにする() {
        // Arrange
        val program = sampleProgramWithWork(workId = "work-fin", title = "フィナーレテスト", isFinale = true)
        coEvery { annictRepository.getRawProgramsData() } returns flowOf(createMockProgramData(program))
        coEvery { judgeFinaleUseCase.invoke(any(), any()) } returns JudgeFinaleResult(
            FinaleState.FINALE_CONFIRMED,
            true
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }
        viewModel.refresh()
        composeTestRule.mainClock.advanceTimeBy(3000)

        // Act
        composeTestRule.onNodeWithContentDescription("record-ep1").performClick()
        composeTestRule.mainClock.advanceTimeBy(2000)

        // Assert
        composeTestRule.onNodeWithTag("finale_confirmation_snackbar").assertIsDisplayed()
        composeTestRule.onNodeWithText("はい").performClick()

        coVerify { annictRepository.updateWorkViewStatus("work-fin", StatusState.WATCHED) }
    }

    private fun createMockProgramData(program: ProgramWithWork): List<com.annict.ViewerProgramsQuery.Node?> = listOf(
        mockk {
            every { id } returns program.firstProgram.id
            every { startedAt } returns program.firstProgram.startedAt.toString()
            every { channel } returns mockk {
                every { name } returns program.firstProgram.channel.name
            }
            every { episode } returns mockk {
                every { id } returns program.firstProgram.episode.id
                every { number } returns program.firstProgram.episode.number
                every { numberText } returns program.firstProgram.episode.numberText
                every { title } returns program.firstProgram.episode.title
            }
            every { work } returns mockk {
                every { id } returns program.work.id
                every { title } returns program.work.title
                every { seasonName } returns program.work.seasonName
                every { seasonYear } returns program.work.seasonYear
                every { media } returns Media.safeValueOf(program.work.media.orEmpty())
                every { malAnimeId } returns (program.work.malAnimeId.orEmpty())
                every { viewerStatusState } returns program.work.viewerStatusState
                every { image } returns mockk {
                    every { recommendedImageUrl } returns "https://example.com/image.jpg"
                    every { facebookOgImageUrl } returns "https://example.com/og_image.jpg"
                }
            }
        }
    )

    @Test
    fun trackScreen_フィルターボタンクリック_フィルターが表示される() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = {},
                    onRefresh = viewModel::refresh
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("フィルター").performClick()

        composeTestRule.onNodeWithText("視聴ステータス").assertIsDisplayed()
    }

    @Test
    fun trackScreen_履歴ナビゲーション_コールバックが呼ばれる() {
        val mockOnNavigateToHistory = mockk<() -> Unit>(relaxed = true)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTestMode provides true) {
                val uiState = viewModel.uiState.collectAsState().value
                TrackScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onRecordEpisode = viewModel::recordEpisode,
                    onNavigateToHistory = mockOnNavigateToHistory,
                    onRefresh = viewModel::refresh
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("履歴").performClick()

        verify { mockOnNavigateToHistory() }
    }
}
