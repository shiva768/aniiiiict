package com.zelretch.aniiiiict.ui.animedetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.annict.WorkDetailQuery
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AniListRepository
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.di.AppModule
import com.zelretch.aniiiiict.domain.filter.ProgramFilter
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import com.zelretch.aniiiiict.testing.HiltComposeTestRule
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiict.ui.theme.AniiiiictTheme
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Integration test for AnimeDetailScreen.
 * Tests the collaboration between ViewModel and UseCase with real dependencies.
 *
 * Following AGENTS.md strategy:
 * - Mock external boundaries (Repository)
 * - Do NOT mock domain UseCases
 * - Prefer verifying outcomes rather than internal method calls
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class AnimeDetailScreenIntegrationTest {

    @get:Rule
    val testRule = HiltComposeTestRule(this)

    // --- Hilt Bindings for Test ---
    @BindValue
    @JvmField
    val mockAnnictRepository: AnnictRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockAniListRepository: AniListRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockProgramFilter: ProgramFilter = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockCustomTabsIntentFactory: CustomTabsIntentFactory = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockMyAnimeListRepository: MyAnimeListRepository = object : MyAnimeListRepository {
        override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> {
            // デフォルトの実装（各テストでオーバーライドする）
            return Result.success(
                MyAnimeListResponse(
                    id = animeId,
                    mediaType = "tv",
                    numEpisodes = 9999,
                    status = "currently_airing",
                    broadcast = null,
                    mainPicture = null
                )
            )
        }
    }

    @Test
    fun 実際のViewModelとモックRepositoryでアニメ詳細が正常にロードされる() {
        // Arrange
        val workId = "test-work-id"
        val malAnimeId = "12345"
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId)

        val mockAnnictDetail = createMockAnnictDetail(workId)
        val mockMalInfo = createMockMalInfo(malAnimeId.toInt())

        // Fake Annict Repository
        val fakeAnnictRepository = object : AnnictRepository {
            override suspend fun getWorkDetail(id: String): Result<WorkDetailQuery.Node?> =
                if (id == workId) Result.success(mockAnnictDetail) else Result.failure(Exception("Not found"))

            override suspend fun isAuthenticated(): Boolean = true
            override suspend fun getAuthUrl(): String = "https://example.com/auth"
            override suspend fun handleAuthCallback(code: String): Boolean = true
            override suspend fun createRecord(episodeId: String, workId: String): Boolean = true
            override suspend fun getRawProgramsData() =
                kotlinx.coroutines.flow.flowOf(emptyList<com.annict.ViewerProgramsQuery.Node>())
            override suspend fun getRecords(after: String?) =
                com.zelretch.aniiiiict.data.model.PaginatedRecords(emptyList(), false, null)
            override suspend fun deleteRecord(recordId: String): Boolean = true
            override suspend fun updateWorkViewStatus(workId: String, status: StatusState): Boolean = true
        }

        // Fake MyAnimeList Repository
        val fakeMyAnimeListRepository = object : MyAnimeListRepository {
            override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> = Result.success(mockMalInfo)
        }

        // 実際のUseCaseを使用してViewModelを生成
        val useCase = GetAnimeDetailUseCase(fakeAnnictRepository, fakeMyAnimeListRepository)
        val viewModel = AnimeDetailViewModel(useCase)

        // Act
        testRule.composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = viewModel
                )
            }
        }

        // LaunchedEffectが実行されるのを待つ
        testRule.composeTestRule.waitForIdle()

        // Wait for loading to complete
        testRule.composeTestRule.waitUntil(timeoutMillis = 10_000) {
            !viewModel.state.value.isLoading
        }

        // Assert: アニメ詳細が正常に表示される
        testRule.composeTestRule.onNodeWithText("全24話").assertIsDisplayed()
        testRule.composeTestRule.onNodeWithText("基本情報").assertIsDisplayed()
    }

    @Test
    fun Annict失敗時にエラーメッセージが表示される() {
        // Arrange
        val workId = "test-work-id"
        val malAnimeId = "12345"
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId)

        // Fake Annict Repository (エラーを返す)
        val fakeAnnictRepository = object : AnnictRepository {
            override suspend fun getWorkDetail(id: String): Result<WorkDetailQuery.Node?> =
                Result.failure(Exception("Annict API Error"))

            override suspend fun isAuthenticated(): Boolean = true
            override suspend fun getAuthUrl(): String = "https://example.com/auth"
            override suspend fun handleAuthCallback(code: String): Boolean = true
            override suspend fun createRecord(episodeId: String, workId: String): Boolean = true
            override suspend fun getRawProgramsData() =
                kotlinx.coroutines.flow.flowOf(emptyList<com.annict.ViewerProgramsQuery.Node>())
            override suspend fun getRecords(after: String?) =
                com.zelretch.aniiiiict.data.model.PaginatedRecords(emptyList(), false, null)
            override suspend fun deleteRecord(recordId: String): Boolean = true
            override suspend fun updateWorkViewStatus(workId: String, status: StatusState): Boolean = true
        }

        // Fake MyAnimeList Repository
        val fakeMyAnimeListRepository = object : MyAnimeListRepository {
            override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> =
                Result.success(createMockMalInfo(malAnimeId.toInt()))
        }

        // 実際のUseCaseを使用してViewModelを生成
        val useCase = GetAnimeDetailUseCase(fakeAnnictRepository, fakeMyAnimeListRepository)
        val viewModel = AnimeDetailViewModel(useCase)

        // Act
        testRule.composeTestRule.setContent {
            AniiiiictTheme {
                AnimeDetailScreen(
                    programWithWork = programWithWork,
                    onNavigateBack = {},
                    viewModel = viewModel
                )
            }
        }

        // LaunchedEffectが実行されるのを待つ
        testRule.composeTestRule.waitForIdle()

        // Wait for loading to complete
        testRule.composeTestRule.waitUntil(timeoutMillis = 10_000) {
            !viewModel.state.value.isLoading
        }

        // Assert: エラーメッセージが表示される（BaseViewModelのErrorHandler経由）
        testRule.composeTestRule.onNodeWithText(
            "処理中にエラーが発生しました",
            substring = true
        ).assertIsDisplayed()
    }

    private fun createSampleProgramWithWork(workId: String, malAnimeId: String?): ProgramWithWork {
        val work = Work(
            id = workId,
            title = "テストアニメ",
            seasonName = SeasonName.SPRING,
            seasonYear = 2024,
            media = "tv",
            malAnimeId = malAnimeId,
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

    private fun createMockAnnictDetail(workId: String): WorkDetailQuery.Node {
        val mockImage = mockk<WorkDetailQuery.Image>()
        every { mockImage.recommendedImageUrl } returns "https://example.com/image.jpg"
        every { mockImage.facebookOgImageUrl } returns null

        val mockPrograms = mockk<WorkDetailQuery.Programs>()
        every { mockPrograms.nodes } returns emptyList()

        val mockSeriesList = mockk<WorkDetailQuery.SeriesList>()
        every { mockSeriesList.nodes } returns emptyList()

        val mockOnWork = mockk<WorkDetailQuery.OnWork>()
        every { mockOnWork.id } returns workId
        every { mockOnWork.episodesCount } returns 12
        every { mockOnWork.image } returns mockImage
        every { mockOnWork.officialSiteUrl } returns "https://example.com/official"
        every { mockOnWork.wikipediaUrl } returns "https://ja.wikipedia.org/wiki/テストアニメ"
        every { mockOnWork.programs } returns mockPrograms
        every { mockOnWork.seriesList } returns mockSeriesList

        val mockNode = mockk<WorkDetailQuery.Node>()
        every { mockNode.onWork } returns mockOnWork

        return mockNode
    }

    private fun createMockMalInfo(malAnimeId: Int): MyAnimeListResponse = MyAnimeListResponse(
        id = malAnimeId,
        mediaType = "tv",
        numEpisodes = 24,
        status = "finished_airing",
        broadcast = null,
        mainPicture = null
    )
}
