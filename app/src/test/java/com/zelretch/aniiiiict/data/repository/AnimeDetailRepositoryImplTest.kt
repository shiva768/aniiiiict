package com.zelretch.aniiiiict.data.repository

import com.annict.WorkDetailQuery
import com.annict.type.Media
import com.annict.type.SeasonName
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.zelretch.aniiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiict.data.model.MyAnimeListMainPicture
import com.zelretch.aniiiiict.data.model.MyAnimeListResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk

class AnimeDetailRepositoryImplTest : BehaviorSpec({

    val mockAnnictApolloClient = mockk<AnnictApolloClient>()
    val mockMyAnimeListRepository = mockk<MyAnimeListRepository>()
    val repository = AnimeDetailRepositoryImpl(mockAnnictApolloClient, mockMyAnimeListRepository)

    given("anime detail repository") {
        `when`("fetching anime detail with valid work ID") {
            val workId = "work123"
            val malAnimeId = "456"

            val mockWorkData = mockk<WorkDetailQuery.OnWork>()
            coEvery { mockWorkData.id } returns workId
            coEvery { mockWorkData.title } returns "Test Anime"
            coEvery { mockWorkData.titleEn } returns "Test Anime EN"
            coEvery { mockWorkData.titleKana } returns "テストアニメ"
            coEvery { mockWorkData.titleRo } returns "Test Anime Ro"
            coEvery { mockWorkData.episodesCount } returns 12
            coEvery { mockWorkData.noEpisodes } returns false
            coEvery { mockWorkData.officialSiteUrl } returns "https://test.com"
            coEvery { mockWorkData.wikipediaUrl } returns "https://wiki.test.com"
            coEvery { mockWorkData.satisfactionRate } returns 85.5
            coEvery { mockWorkData.watchersCount } returns 1000
            coEvery { mockWorkData.reviewsCount } returns 50
            coEvery { mockWorkData.programs } returns null
            coEvery { mockWorkData.seriesList } returns null

            val mockResponse = mockk<ApolloResponse<WorkDetailQuery.Data>>()
            val mockData = mockk<WorkDetailQuery.Data>()
            val mockNode = mockk<WorkDetailQuery.Node>()

            coEvery { mockNode.onWork } returns mockWorkData
            coEvery { mockData.node } returns mockNode
            coEvery { mockResponse.data } returns mockData
            coEvery { mockResponse.hasErrors() } returns false

            coEvery { mockAnnictApolloClient.executeQuery(any<WorkDetailQuery>(), any()) } returns mockResponse

            val mockMalResponse = MyAnimeListResponse(
                id = 456,
                mediaType = "tv",
                numEpisodes = 12,
                status = "finished_airing",
                broadcast = null,
                mainPicture = MyAnimeListMainPicture(
                    medium = "https://mal.test.com/medium.jpg",
                    large = "https://mal.test.com/large.jpg"
                )
            )

            coEvery { mockMyAnimeListRepository.getAnimeDetail(456) } returns Result.success(mockMalResponse)

            then("should return anime detail info successfully") {
                val result = repository.getAnimeDetailInfo(workId, malAnimeId)

                result.isSuccess shouldBe true
                val animeDetailInfo = result.getOrNull()
                animeDetailInfo shouldNotBe null
                animeDetailInfo!!.workId shouldBe workId
                animeDetailInfo.title shouldBe "Test Anime"
                animeDetailInfo.titleEn shouldBe "Test Anime EN"
                animeDetailInfo.episodesCount shouldBe 12
                animeDetailInfo.malEpisodeCount shouldBe 12
                animeDetailInfo.malImageUrl shouldBe "https://mal.test.com/large.jpg"
                animeDetailInfo.officialSiteUrl shouldBe "https://test.com"
                animeDetailInfo.wikipediaUrl shouldBe "https://wiki.test.com"
                animeDetailInfo.satisfactionRate shouldBe 85.5f
                animeDetailInfo.watchersCount shouldBe 1000
                animeDetailInfo.reviewsCount shouldBe 50
            }
        }

        `when`("GraphQL query fails") {
            val workId = "work123"
            val malAnimeId = "456"

            val mockResponse = mockk<ApolloResponse<WorkDetailQuery.Data>>()
            val mockError = mockk<Error>()
            coEvery { mockError.message } returns "Test error"
            coEvery { mockResponse.hasErrors() } returns true
            coEvery { mockResponse.errors } returns listOf(mockError)

            coEvery { mockAnnictApolloClient.executeQuery(any<WorkDetailQuery>(), any()) } returns mockResponse

            then("should return failure") {
                val result = repository.getAnimeDetailInfo(workId, malAnimeId)

                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Failed to fetch work details: Test error"
            }
        }

        `when`("work data is null") {
            val workId = "work123"
            val malAnimeId = "456"

            val mockResponse = mockk<ApolloResponse<WorkDetailQuery.Data>>()
            val mockData = mockk<WorkDetailQuery.Data>()
            val mockNode = mockk<WorkDetailQuery.Node>()

            coEvery { mockNode.onWork } returns null
            coEvery { mockData.node } returns mockNode
            coEvery { mockResponse.data } returns mockData
            coEvery { mockResponse.hasErrors() } returns false

            coEvery { mockAnnictApolloClient.executeQuery(any<WorkDetailQuery>(), any()) } returns mockResponse

            then("should return failure") {
                val result = repository.getAnimeDetailInfo(workId, malAnimeId)

                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Work not found or invalid response"
            }
        }

        `when`("MAL data is not available") {
            val workId = "work123"
            val malAnimeId = null

            val mockWorkData = mockk<WorkDetailQuery.OnWork>()
            coEvery { mockWorkData.id } returns workId
            coEvery { mockWorkData.title } returns "Test Anime"
            coEvery { mockWorkData.titleEn } returns null
            coEvery { mockWorkData.titleKana } returns null
            coEvery { mockWorkData.titleRo } returns null
            coEvery { mockWorkData.episodesCount } returns 12
            coEvery { mockWorkData.noEpisodes } returns false
            coEvery { mockWorkData.officialSiteUrl } returns null
            coEvery { mockWorkData.wikipediaUrl } returns null
            coEvery { mockWorkData.satisfactionRate } returns null
            coEvery { mockWorkData.watchersCount } returns 100
            coEvery { mockWorkData.reviewsCount } returns 5
            coEvery { mockWorkData.programs } returns null
            coEvery { mockWorkData.seriesList } returns null

            val mockResponse = mockk<ApolloResponse<WorkDetailQuery.Data>>()
            val mockData = mockk<WorkDetailQuery.Data>()
            val mockNode = mockk<WorkDetailQuery.Node>()

            coEvery { mockNode.onWork } returns mockWorkData
            coEvery { mockData.node } returns mockNode
            coEvery { mockResponse.data } returns mockData
            coEvery { mockResponse.hasErrors() } returns false

            coEvery { mockAnnictApolloClient.executeQuery(any<WorkDetailQuery>(), any()) } returns mockResponse

            then("should return anime detail info without MAL data") {
                val result = repository.getAnimeDetailInfo(workId, malAnimeId)

                result.isSuccess shouldBe true
                val animeDetailInfo = result.getOrNull()
                animeDetailInfo shouldNotBe null
                animeDetailInfo!!.workId shouldBe workId
                animeDetailInfo.title shouldBe "Test Anime"
                animeDetailInfo.malEpisodeCount shouldBe null
                animeDetailInfo.malImageUrl shouldBe null
            }
        }
    }
})