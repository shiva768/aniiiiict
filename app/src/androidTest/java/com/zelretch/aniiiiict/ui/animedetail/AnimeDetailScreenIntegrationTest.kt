package com.zelretch.aniiiiict.ui.animedetail

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
import com.zelretch.aniiiiict.ui.base.CustomTabsIntentFactory
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Integration test for GetAnimeDetailUseCase.
 * Tests the collaboration between UseCase and Repository with real dependencies.
 *
 * Following CLAUDE.md strategy:
 * - Mock external boundaries (Repository)
 * - Do NOT mock domain UseCases
 * - Test UseCase+Repository collaboration
 */
@HiltAndroidTest
@UninstallModules(AppModule::class)
class AnimeDetailScreenIntegrationTest {

    @BindValue
    @JvmField
    val mockAnnictRepository: AnnictRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockAniListRepository: AniListRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockMyAnimeListRepository: MyAnimeListRepository = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockProgramFilter: ProgramFilter = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockCustomTabsIntentFactory: CustomTabsIntentFactory = mockk(relaxed = true)

    @Test
    fun UseCase_複数Repositoryから正しくデータを取得できる() = runTest {
        // Arrange
        val workId = "test-work-id"
        val malAnimeId = 12345

        val mockAnnictDetail = createMockAnnictDetail(workId)
        val mockMalInfo = createMockMalInfo(malAnimeId)

        // Fake Annict Repository
        val fakeAnnictRepository = object : AnnictRepository {
            override suspend fun getWorkDetail(id: String): Result<WorkDetailQuery.Node?> =
                if (id == workId) Result.success(mockAnnictDetail) else Result.failure(Exception("Not found"))

            override suspend fun isAuthenticated(): Boolean = true
            override suspend fun getAuthUrl(): String = "https://example.com/auth"
            override suspend fun handleAuthCallback(code: String): Result<Unit> = Result.success(Unit)
            override suspend fun createRecord(episodeId: String, workId: String): Result<Unit> = Result.success(Unit)
            override suspend fun getRawProgramsData(): Result<List<com.annict.ViewerProgramsQuery.Node?>> =
                Result.success(emptyList())
            override suspend fun getRecords(
                after: String?
            ): Result<com.zelretch.aniiiiict.data.model.PaginatedRecords> =
                Result.success(com.zelretch.aniiiiict.data.model.PaginatedRecords(emptyList(), false, null))
            override suspend fun deleteRecord(recordId: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateWorkViewStatus(workId: String, status: StatusState): Result<Unit> =
                Result.success(Unit)
            override suspend fun getLibraryEntries(
                states: List<StatusState>,
                after: String?
            ): Result<List<com.zelretch.aniiiiict.data.model.LibraryEntry>> = Result.success(emptyList())
        }

        // Fake MyAnimeList Repository
        val fakeMyAnimeListRepository = object : MyAnimeListRepository {
            override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> = Result.success(mockMalInfo)
        }

        val useCase = GetAnimeDetailUseCase(fakeAnnictRepository, fakeMyAnimeListRepository)
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId.toString())

        // Act
        val result = useCase(programWithWork)

        // Assert: UseCaseが成功を返す
        assertTrue(result.isSuccess)
        val animeDetail = result.getOrNull()
        assertNotNull(animeDetail)
        assertEquals(24, animeDetail?.episodeCount)
    }

    @Test
    fun UseCase_Annict失敗時はエラーを正しく伝播する() = runTest {
        // Arrange
        val workId = "test-work-id"
        val malAnimeId = 12345

        // Fake Annict Repository (エラーを返す)
        val fakeAnnictRepository = object : AnnictRepository {
            override suspend fun getWorkDetail(id: String): Result<WorkDetailQuery.Node?> =
                Result.failure(Exception("Annict API Error"))

            override suspend fun isAuthenticated(): Boolean = true
            override suspend fun getAuthUrl(): String = "https://example.com/auth"
            override suspend fun handleAuthCallback(code: String): Result<Unit> = Result.success(Unit)
            override suspend fun createRecord(episodeId: String, workId: String): Result<Unit> = Result.success(Unit)
            override suspend fun getRawProgramsData(): Result<List<com.annict.ViewerProgramsQuery.Node?>> =
                Result.success(emptyList())
            override suspend fun getRecords(
                after: String?
            ): Result<com.zelretch.aniiiiict.data.model.PaginatedRecords> =
                Result.success(com.zelretch.aniiiiict.data.model.PaginatedRecords(emptyList(), false, null))
            override suspend fun deleteRecord(recordId: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateWorkViewStatus(workId: String, status: StatusState): Result<Unit> =
                Result.success(Unit)
            override suspend fun getLibraryEntries(
                states: List<StatusState>,
                after: String?
            ): Result<List<com.zelretch.aniiiiict.data.model.LibraryEntry>> = Result.success(emptyList())
        }

        // Fake MyAnimeList Repository
        val fakeMyAnimeListRepository = object : MyAnimeListRepository {
            override suspend fun getAnimeDetail(animeId: Int): Result<MyAnimeListResponse> =
                Result.success(createMockMalInfo(malAnimeId))
        }

        val useCase = GetAnimeDetailUseCase(fakeAnnictRepository, fakeMyAnimeListRepository)
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId.toString())

        // Act
        val result = useCase(programWithWork)

        // Assert: UseCaseが失敗を返す
        assertTrue(result.isFailure)
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
            programs = listOf(program)
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
