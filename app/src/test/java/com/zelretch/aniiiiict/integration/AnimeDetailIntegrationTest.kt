package com.zelretch.aniiiiict.integration

import com.annict.WorkDetailQuery
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import com.zelretch.aniiiiict.data.repository.MyAnimeListRepository
import com.zelretch.aniiiiict.domain.usecase.GetAnimeDetailUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Integration test for AnimeDetail feature.
 * Tests the collaboration between GetAnimeDetailUseCase and repositories.
 *
 * Following AGENTS.md strategy:
 * - Mock external boundaries (Repository)
 * - Do NOT mock domain UseCases
 * - Prefer verifying outcomes rather than internal method calls
 */
class AnimeDetailIntegrationTest {

    private lateinit var annictRepository: AnnictRepository
    private lateinit var myAnimeListRepository: MyAnimeListRepository
    private lateinit var getAnimeDetailUseCase: GetAnimeDetailUseCase

    @Before
    fun setup() {
        annictRepository = mockk()
        myAnimeListRepository = mockk()
        getAnimeDetailUseCase = GetAnimeDetailUseCase(annictRepository, myAnimeListRepository)
    }

    @Test
    fun `should successfully integrate Annict and MyAnimeList data`() = runTest {
        // Given
        val workId = "test-work-id"
        val malAnimeId = "12345"
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId)

        val mockAnnictDetail = createMockAnnictDetail(workId)
        val mockMalInfo = createMockMalInfo(malAnimeId.toInt())

        coEvery { annictRepository.getWorkDetail(workId) } returns Result.success(mockAnnictDetail)
        coEvery { myAnimeListRepository.getAnimeDetail(malAnimeId.toInt()) } returns Result.success(mockMalInfo)

        // When
        val result = getAnimeDetailUseCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrThrow()

        // Verify integrated data
        assertEquals(workId, animeDetailInfo.work.id)
        assertEquals("テストアニメ", animeDetailInfo.work.title)
        assertEquals(24, animeDetailInfo.episodeCount) // MyAnimeList優先
        assertEquals("https://example.com/image.jpg", animeDetailInfo.imageUrl)
        assertEquals("https://example.com/official", animeDetailInfo.officialSiteUrl)
        assertEquals("https://ja.wikipedia.org/wiki/テストアニメ", animeDetailInfo.wikipediaUrl)

        // Verify MyAnimeList data integration
        assertNotNull(animeDetailInfo.malInfo)
        assertEquals(24, animeDetailInfo.malInfo?.numEpisodes)
        assertEquals("currently_airing", animeDetailInfo.malInfo?.status)

        // Verify Annict-specific data
        assertNotNull(animeDetailInfo.programs)
        assertNotNull(animeDetailInfo.seriesList)
    }

    @Test
    fun `should handle MyAnimeList API failure gracefully`() = runTest {
        // Given
        val workId = "test-work-id"
        val malAnimeId = "12345"
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId)

        val mockAnnictDetail = createMockAnnictDetail(workId)

        coEvery { annictRepository.getWorkDetail(workId) } returns Result.success(mockAnnictDetail)
        coEvery { myAnimeListRepository.getAnimeDetail(malAnimeId.toInt()) } returns
            Result.failure(Exception("MAL API Error"))

        // When
        val result = getAnimeDetailUseCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrThrow()

        // Should fallback to Annict data
        assertEquals(workId, animeDetailInfo.work.id)
        assertEquals(12, animeDetailInfo.episodeCount) // Annictの値を使用
        assertEquals("https://example.com/image.jpg", animeDetailInfo.imageUrl)
        assertEquals(null, animeDetailInfo.malInfo) // MAL情報は取得失敗
    }

    @Test
    fun `should handle Annict API failure gracefully`() = runTest {
        // Given
        val workId = "test-work-id"
        val malAnimeId = "12345"
        val programWithWork = createSampleProgramWithWork(workId, malAnimeId)

        val mockMalInfo = createMockMalInfo(malAnimeId.toInt())

        coEvery { annictRepository.getWorkDetail(workId) } returns Result.failure(Exception("Annict API Error"))
        coEvery { myAnimeListRepository.getAnimeDetail(malAnimeId.toInt()) } returns Result.success(mockMalInfo)

        // When
        val result = getAnimeDetailUseCase(programWithWork)

        // Then
        assertTrue(result.isFailure) // Annict APIは必須なので失敗
    }

    @Test
    fun `should handle missing malAnimeId gracefully`() = runTest {
        // Given
        val workId = "test-work-id"
        val programWithWork = createSampleProgramWithWork(workId, null) // malAnimeIdなし

        val mockAnnictDetail = createMockAnnictDetail(workId)

        coEvery { annictRepository.getWorkDetail(workId) } returns Result.success(mockAnnictDetail)

        // When
        val result = getAnimeDetailUseCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrThrow()

        assertEquals(workId, animeDetailInfo.work.id)
        assertEquals(12, animeDetailInfo.episodeCount) // Annictの値
        assertEquals(null, animeDetailInfo.malInfo) // MAL情報は取得しない
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

        val mockChannel = mockk<WorkDetailQuery.Channel>()
        every { mockChannel.id } returns "channel-id"
        every { mockChannel.name } returns "テストチャンネル"

        val mockProgram = mockk<WorkDetailQuery.Node1>()
        every { mockProgram.id } returns "program-id"
        every { mockProgram.startedAt } returns "2024-01-01T12:00:00Z"
        every { mockProgram.channel } returns mockChannel

        val mockPrograms = mockk<WorkDetailQuery.Programs>()
        every { mockPrograms.nodes } returns listOf(mockProgram)

        val mockRelatedWork = mockk<WorkDetailQuery.Node3>()
        every { mockRelatedWork.id } returns "related-work-id"
        every { mockRelatedWork.title } returns "関連作品"
        every { mockRelatedWork.titleEn } returns null
        every { mockRelatedWork.seasonName } returns null
        every { mockRelatedWork.seasonYear } returns null
        every { mockRelatedWork.image } returns null

        val mockWorks = mockk<WorkDetailQuery.Works>()
        every { mockWorks.nodes } returns listOf(mockRelatedWork)

        val mockSeries = mockk<WorkDetailQuery.Node2>()
        every { mockSeries.id } returns "series-id"
        every { mockSeries.name } returns "テストシリーズ"
        every { mockSeries.nameEn } returns "Test Series"
        every { mockSeries.works } returns mockWorks

        val mockSeriesList = mockk<WorkDetailQuery.SeriesList>()
        every { mockSeriesList.nodes } returns listOf(mockSeries)

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
        status = "currently_airing",
        broadcast = null
    )
}
