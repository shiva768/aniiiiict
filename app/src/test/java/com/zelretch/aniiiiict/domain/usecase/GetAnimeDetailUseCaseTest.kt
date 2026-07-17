package com.zelretch.aniiiiict.domain.usecase

import com.annict.WorkDetailQuery
import com.annict.WorkSeriesListQuery
import com.annict.type.Media
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("GetAnimeDetailUseCase")
class GetAnimeDetailUseCaseTest {

    private lateinit var annictRepository: AnnictRepository
    private lateinit var myAnimeListRepository: MyAnimeListRepository
    private lateinit var useCase: GetAnimeDetailUseCase

    @BeforeEach
    fun setup() {
        annictRepository = mockk()
        myAnimeListRepository = mockk()
        useCase = GetAnimeDetailUseCase(annictRepository, myAnimeListRepository)
        coEvery { annictRepository.getWorkSeriesList(any()) } returns Result.success(null)
    }

    @Nested
    @DisplayName("正常系")
    inner class SuccessCases {

        @Test
        @DisplayName("AnnictとMyAnimeListのデータを統合して取得できる")
        fun AnnictとMyAnimeListのデータを統合して取得できる() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val annictDetail = createMockAnnictDetail()
            val malResponse = createMockMyAnimeListResponse()
            val seriesListNode = createMockWorkSeriesListNode()

            coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)
            coEvery { annictRepository.getWorkSeriesList(any()) } returns Result.success(seriesListNode)
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
            // キャスト（声優）情報が取得できている
            assertEquals("テストキャラ", animeDetailInfo?.casts?.first()?.character?.name)
            assertEquals("テスト声優", animeDetailInfo?.casts?.first()?.person?.name)

            coVerify { annictRepository.getWorkDetail(programWithWork.work.id) }
            coVerify { myAnimeListRepository.getAnimeDetail(12345) }
        }

        @Test
        @DisplayName("MyAnimeList失敗時もAnnict成功なら正常に取得できる")
        fun MyAnimeList失敗時もAnnict成功なら正常に取得できる() = runTest {
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
        @DisplayName("Annict詳細ノードがnullでもMyAnimeListデータを使用できる")
        fun Annict詳細ノードがnullでもMyAnimeListデータを使用できる() = runTest {
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
    }

    @Nested
    @DisplayName("エピソード数のフォールバック")
    inner class EpisodeCountFallback {

        @Test
        @DisplayName("MyAnimeListデータがない場合Annictエピソード数にフォールバックする")
        fun MyAnimeListデータがない場合Annictエピソード数にフォールバックする() = runTest {
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
        @DisplayName("malAnimeIdがnullの場合MyAnimeList取得をスキップする")
        fun malAnimeIdがnullの場合MyAnimeList取得をスキップする() = runTest {
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
        @DisplayName("エピソード数が0の場合はnullとして扱う（話数不明を意味する）")
        fun エピソード数が0の場合はnullとして扱う() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val mockOnWork = mockk<WorkDetailQuery.OnWork>()
            every { mockOnWork.id } returns "test-work-id"
            every { mockOnWork.episodesCount } returns 0 // 話数不明を示す
            every { mockOnWork.image } returns null
            every { mockOnWork.officialSiteUrl } returns null
            every { mockOnWork.wikipediaUrl } returns null
            every { mockOnWork.programs } returns mockk { every { nodes } returns emptyList() }
            every { mockOnWork.casts } returns null

            val mockNode = mockk<WorkDetailQuery.Node>()
            every { mockNode.onWork } returns mockOnWork

            coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(mockNode)
            coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.failure(Exception("MAL Error"))

            // When
            val result = useCase(programWithWork)

            // Then
            assertTrue(result.isSuccess)
            val animeDetailInfo = result.getOrNull()
            assertNotNull(animeDetailInfo)
            assertNull(animeDetailInfo?.episodeCount) // 0はnullとして扱う
        }

        @Test
        @DisplayName("MyAnimeListのエピソード数が0の場合はAnnictのデータを優先する")
        fun MyAnimeListのエピソード数が0の場合はAnnictのデータを優先する() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val annictDetail = createMockAnnictDetail()
            val malResponse = MyAnimeListResponse(
                id = 12345,
                mediaType = "tv",
                numEpisodes = 0, // 0はスキップ
                status = "currently_airing",
                broadcast = null,
                mainPicture = null
            )

            coEvery { annictRepository.getWorkDetail(any()) } returns Result.success(annictDetail)
            coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

            // When
            val result = useCase(programWithWork)

            // Then
            assertTrue(result.isSuccess)
            val animeDetailInfo = result.getOrNull()
            assertNotNull(animeDetailInfo)
            assertEquals(12, animeDetailInfo?.episodeCount) // Annictのデータが使用される
        }
    }

    @Nested
    @DisplayName("画像のフォールバック")
    inner class ImageFallback {

        @Test
        @DisplayName("Annict画像がnullの場合MyAnimeList画像にフォールバックする")
        fun Annict画像がnullの場合MyAnimeList画像にフォールバックする() = runTest {
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
            every { mockOnWork.casts } returns null

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
    }

    @Nested
    @DisplayName("エラーケース")
    inner class ErrorCases {

        @Test
        @DisplayName("Annict取得失敗時はエラーを返す")
        fun Annict取得失敗時はエラーを返す() = runTest {
            // Given
            val programWithWork = createSampleProgramWithWork()
            val malResponse = createMockMyAnimeListResponse()

            coEvery { annictRepository.getWorkDetail(any()) } returns Result.failure(Exception("Annict API Error"))
            coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

            // When
            val result = useCase(programWithWork)

            // Then
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("workIdのみでの取得")
    inner class InvokeByWorkId {

        @Test
        @DisplayName("workIdのみでAnnictとMyAnimeListデータを統合して取得できる")
        fun workIdのみでデータを取得できる() = runTest {
            // Given
            val annictDetail = createMockAnnictDetailWithFullFields()
            val malResponse = createMockMyAnimeListResponse()

            coEvery { annictRepository.getWorkDetail("test-work-id") } returns Result.success(annictDetail)
            coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.success(malResponse)

            // When
            val result = useCase("test-work-id")

            // Then
            assertTrue(result.isSuccess)
            val animeDetailInfo = result.getOrNull()
            assertNotNull(animeDetailInfo)
            assertEquals("テストアニメ", animeDetailInfo?.work?.title)
            assertEquals(StatusState.WATCHING, animeDetailInfo?.work?.viewerStatusState)
            assertEquals(24, animeDetailInfo?.episodeCount)
            assertEquals("https://annict.example.com/image.jpg", animeDetailInfo?.imageUrl)
        }

        @Test
        @DisplayName("MyAnimeList失敗時もAnnict成功ならworkIdのみでも取得できる")
        fun MyAnimeList失敗時もworkIdのみで取得できる() = runTest {
            // Given
            val annictDetail = createMockAnnictDetailWithFullFields()

            coEvery { annictRepository.getWorkDetail("test-work-id") } returns Result.success(annictDetail)
            coEvery { myAnimeListRepository.getAnimeDetail(12345) } returns Result.failure(Exception("MAL Error"))

            // When
            val result = useCase("test-work-id")

            // Then
            assertTrue(result.isSuccess)
            val animeDetailInfo = result.getOrNull()
            assertNotNull(animeDetailInfo)
            assertNull(animeDetailInfo?.malInfo)
            assertEquals(12, animeDetailInfo?.episodeCount)
        }

        @Test
        @DisplayName("Annict取得失敗時はエラーを返す")
        fun Annict取得失敗時はworkIdのみでエラーを返す() = runTest {
            // Given
            coEvery { annictRepository.getWorkDetail("test-work-id") } returns
                Result.failure(Exception("Annict API Error"))

            // When
            val result = useCase("test-work-id")

            // Then
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }

        @Test
        @DisplayName("onWorkがnullの場合はエラーを返す")
        fun onWorkがnullの場合はエラーを返す() = runTest {
            // Given
            val mockNode = mockk<WorkDetailQuery.Node>()
            every { mockNode.onWork } returns null

            coEvery { annictRepository.getWorkDetail("test-work-id") } returns Result.success(mockNode)

            // When
            val result = useCase("test-work-id")

            // Then
            assertTrue(result.isFailure)
            assertNotNull(result.exceptionOrNull())
        }
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
            programs = listOf(program)
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

        val mockCharacter = mockk<WorkDetailQuery.Character>()
        every { mockCharacter.name } returns "テストキャラ"
        val mockPerson = mockk<WorkDetailQuery.Person>()
        every { mockPerson.name } returns "テスト声優"
        val mockCast = mockk<WorkDetailQuery.Node2>()
        every { mockCast.id } returns "cast-id"
        every { mockCast.name } returns "テスト声優"
        every { mockCast.character } returns mockCharacter
        every { mockCast.person } returns mockPerson
        val mockCasts = mockk<WorkDetailQuery.Casts>()
        every { mockCasts.nodes } returns listOf(mockCast)
        every { mockOnWork.casts } returns mockCasts

        val mockNode = mockk<WorkDetailQuery.Node>()
        every { mockNode.onWork } returns mockOnWork

        return mockNode
    }

    private fun createMockWorkSeriesListNode(): WorkSeriesListQuery.Node {
        val mockWork = mockk<WorkSeriesListQuery.Node2>()
        every { mockWork.id } returns "related-work-id"
        every { mockWork.title } returns "関連作品"
        every { mockWork.titleEn } returns "Related Work"
        every { mockWork.seasonName } returns null
        every { mockWork.seasonYear } returns null
        every { mockWork.image } returns null

        val mockWorks = mockk<WorkSeriesListQuery.Works>()
        every { mockWorks.nodes } returns listOf(mockWork)

        val mockSeries = mockk<WorkSeriesListQuery.Node1>()
        every { mockSeries.id } returns "series-id"
        every { mockSeries.name } returns "テストシリーズ"
        every { mockSeries.nameEn } returns "Test Series"
        every { mockSeries.works } returns mockWorks

        val mockSeriesList = mockk<WorkSeriesListQuery.SeriesList>()
        every { mockSeriesList.nodes } returns listOf(mockSeries)

        val mockOnWork = mockk<WorkSeriesListQuery.OnWork>()
        every { mockOnWork.seriesList } returns mockSeriesList

        val mockNode = mockk<WorkSeriesListQuery.Node>()
        every { mockNode.onWork } returns mockOnWork

        return mockNode
    }

    private fun createMockAnnictDetailWithFullFields(): WorkDetailQuery.Node {
        val mockOnWork = mockk<WorkDetailQuery.OnWork>()
        every { mockOnWork.id } returns "test-work-id"
        every { mockOnWork.title } returns "テストアニメ"
        every { mockOnWork.titleEn } returns null
        every { mockOnWork.seasonName } returns SeasonName.SPRING
        every { mockOnWork.seasonYear } returns 2024
        every { mockOnWork.media } returns Media.TV
        every { mockOnWork.malAnimeId } returns "12345"
        every { mockOnWork.viewerStatusState } returns StatusState.WATCHING
        every { mockOnWork.episodesCount } returns 12
        every { mockOnWork.officialSiteUrl } returns "https://example.com/official"
        every { mockOnWork.wikipediaUrl } returns "https://ja.wikipedia.org/wiki/test"

        val mockImage = mockk<WorkDetailQuery.Image>()
        every { mockImage.recommendedImageUrl } returns "https://annict.example.com/image.jpg"
        every { mockImage.facebookOgImageUrl } returns null
        every { mockOnWork.image } returns mockImage

        every { mockOnWork.programs } returns mockk { every { nodes } returns emptyList() }
        every { mockOnWork.casts } returns null

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
