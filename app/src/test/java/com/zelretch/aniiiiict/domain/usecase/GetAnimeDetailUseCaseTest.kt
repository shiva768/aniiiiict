package com.zelretch.aniiiiict.domain.usecase

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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetAnimeDetailUseCaseTest {

    private lateinit var annictRepository: AnnictRepository
    private lateinit var myAnimeListRepository: MyAnimeListRepository
    private lateinit var useCase: GetAnimeDetailUseCase

    @Before
    fun setup() {
        annictRepository = mockk()
        myAnimeListRepository = mockk()
        useCase = GetAnimeDetailUseCase(annictRepository, myAnimeListRepository)
    }

    @Test
    fun `invoke should successfully combine Annict and MyAnimeList data`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val annictDetail = createMockAnnictDetail()
        val malResponse = createMockMyAnimeListResponse()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        assertEquals("テストアニメ", animeDetailInfo?.work?.title)
        assertEquals(24, animeDetailInfo?.episodeCount) // MALのデータが優先
        assertEquals("https://annict.example.com/image.jpg", animeDetailInfo?.imageUrl)
        assertEquals("https://example.com/official", animeDetailInfo?.officialSiteUrl)
        assertEquals("https://ja.wikipedia.org/wiki/test", animeDetailInfo?.wikipediaUrl)
        assertNotNull(animeDetailInfo?.programs)
        assertNotNull(animeDetailInfo?.seriesList)
        assertNotNull(animeDetailInfo?.malInfo)

        coVerify { annictRepository.getWorkDetail(programWithWork.work.id) }
        coVerify { myAnimeListRepository.getAnimeDetail(12345) }
    }

    @Test
    fun `invoke should fallback to Annict episodeCount when MAL data is unavailable`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val annictDetail = createMockAnnictDetail()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.failure(Exception("MAL Error"))

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        assertEquals(12, animeDetailInfo?.episodeCount) // Annictのデータにフォールバック
        assertNull(animeDetailInfo?.malInfo)
    }

    @Test
    fun `invoke should handle missing malAnimeId gracefully`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork(malAnimeId = null)
        val annictDetail = createMockAnnictDetail()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        assertNull(animeDetailInfo?.malInfo)
        assertEquals(12, animeDetailInfo?.episodeCount) // Annictのみ

        coVerify(exactly = 0) { myAnimeListRepository.getAnimeDetail(any()) }
    }

    @Test
    fun `invoke should succeed when MyAnimeList fails but Annict succeeds`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val annictDetail = createMockAnnictDetail()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.failure(Exception("MAL API Error"))

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        assertEquals(12, animeDetailInfo?.episodeCount) // Annictのデータにフォールバック
        assertNull(animeDetailInfo?.malInfo) // MALデータはnull
    }

    @Test
    fun `invoke should fail when Annict repository fails`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val exception = Exception("Annict API Error")
        val malResponse = createMockMyAnimeListResponse()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.failure(exception)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke should handle null Annict detail node`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val malResponse = createMockMyAnimeListResponse()

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(null)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        assertEquals(24, animeDetailInfo?.episodeCount) // MALのデータを使用
        assertNull(animeDetailInfo?.officialSiteUrl) // Annictデータがnullなのでnull
        assertNull(animeDetailInfo?.wikipediaUrl) // Annictデータがnullなのでnull
    }

    @Test
    fun `invoke should fallback to MAL image when Annict image is null`() = runTest {
        // Given
        val programWithWork = createSampleProgramWithWork()
        val annictDetail = createMockAnnictDetail()
        val malResponse = createMockMyAnimeListResponse()

        // Annictの画像をnullに設定
        val mockOnWork = mockk<WorkDetailQuery.OnWork>()
        every { mockOnWork.id } returns "test-work-id"
        every { mockOnWork.episodesCount } returns 12
        every { mockOnWork.image } returns null // 画像なし
        every { mockOnWork.officialSiteUrl } returns "https://example.com/official"
        every { mockOnWork.wikipediaUrl } returns "https://ja.wikipedia.org/wiki/test"
        every { mockOnWork.programs } returns mockk { every { nodes } returns emptyList() }
        every { mockOnWork.seriesList } returns mockk { every { nodes } returns emptyList() }

        val mockNode = mockk<WorkDetailQuery.Node>()
        every { mockNode.onWork } returns mockOnWork

        coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(mockNode)
        coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

        // When
        val result = useCase(programWithWork)

        // Then
        assertTrue(result.isSuccess)
        val animeDetailInfo = result.getOrNull()
        assertNotNull(animeDetailInfo)
        // MALのlarge画像が使用される
        assertEquals("https://cdn.myanimelist.net/images/anime/large.jpg", animeDetailInfo?.imageUrl)
    }

    private fun createSampleProgramWithWork(malAnimeId: String? = "12345"): ProgramWithWork {
        val work = Work(
            id = "test-work-id",
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

    private fun createMockAnnictDetail(): WorkDetailQuery.Node {
        val mockOnWork = mockk<WorkDetailQuery.OnWork>()
        every { mockOnWork.id } returns "test-work-id"
        every { mockOnWork.episodesCount } returns 12
        every { mockOnWork.officialSiteUrl } returns "https://example.com/official"
        every { mockOnWork.wikipediaUrl } returns "https://ja.wikipedia.org/wiki/test"

        val mockImage = mockk<WorkDetailQuery.Image>()
        every { mockImage.recommendedImageUrl } returns "https://annict.example.com/image.jpg"
        every { mockOnWork.image } returns mockImage

        val mockChannel = mockk<WorkDetailQuery.Channel>()
        every { mockChannel.id } returns "channel-id"
        every { mockChannel.name } returns "テストチャンネル"

        val mockProgram = mockk<WorkDetailQuery.Node1>()
        every { mockProgram.id } returns "program-id"
        every { mockProgram.startedAt } returns "2024-01-01T12:00:00Z"
        every { mockProgram.channel } returns mockChannel

        val mockPrograms = mockk<WorkDetailQuery.Programs>()
        every { mockPrograms.nodes } returns listOf(mockProgram)
        every { mockOnWork.programs } returns mockPrograms

        val mockWork = mockk<WorkDetailQuery.Node3>()
        every { mockWork.id } returns "related-work-id"
        every { mockWork.title } returns "関連作品"
        every { mockWork.titleEn } returns "Related Work"
        every { mockWork.seasonName } returns null
        every { mockWork.seasonYear } returns null
        every { mockWork.image } returns null

        val mockWorks = mockk<WorkDetailQuery.Works>()
        every { mockWorks.nodes } returns listOf(mockWork)

        val mockSeries = mockk<WorkDetailQuery.Node2>()
        every { mockSeries.id } returns "series-id"
        every { mockSeries.name } returns "テストシリーズ"
        every { mockSeries.nameEn } returns "Test Series"
        every { mockSeries.works } returns mockWorks

        val mockSeriesList = mockk<WorkDetailQuery.SeriesList>()
        every { mockSeriesList.nodes } returns listOf(mockSeries)
        every { mockOnWork.seriesList } returns mockSeriesList

        val mockNode = mockk<WorkDetailQuery.Node>()
        every { mockNode.onWork } returns mockOnWork

        return mockNode
    }

    private fun createMockMyAnimeListResponse(): MyAnimeListResponse {
        val mainPicture = com.zelretch.aniiiiict.data.model.MyAnimeListPicture(
            medium = "https://cdn.myanimelist.net/images/anime/medium.jpg",
            large = "https://cdn.myanimelist.net/images/anime/large.jpg"
        )
        return MyAnimeListResponse(
            id = 12345,
            mediaType = "tv",
            numEpisodes = 24,
            status = "currently_airing",
            broadcast = null,
            mainPicture = mainPicture
        )
    }
}
