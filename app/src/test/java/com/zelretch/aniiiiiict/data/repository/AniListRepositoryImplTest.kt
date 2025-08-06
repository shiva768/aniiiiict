package com.zelretch.aniiiiiict.data.repository

import co.anilist.GetMediaQuery
import co.anilist.type.MediaFormat
import co.anilist.type.MediaStatus
import com.apollographql.apollo.api.ApolloResponse
import com.benasher44.uuid.Uuid
import com.zelretch.aniiiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiiict.data.model.AniListMedia
import com.zelretch.aniiiiiict.data.model.NextAiringEpisode
import com.zelretch.aniiiiiict.util.TestLogger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class AniListRepositoryImplTest : BehaviorSpec({

    val mockApolloClient = mockk<AniListApolloClient>()
    val logger = TestLogger()
    val repository = AniListRepositoryImpl(mockApolloClient, logger)

    given("AniListRepositoryImpl の getMedia メソッド") {
        `when`("API呼び出しが成功した場合") {
            then("AniListMediaを返す") {
                val mediaId = 123
                val operation = GetMediaQuery(mediaId)
                val expectedMedia = GetMediaQuery.Data(
                    GetMediaQuery.Media(
                        id = mediaId,
                        format = MediaFormat.TV,
                        episodes = 12,
                        status = MediaStatus.RELEASING,
                        nextAiringEpisode = GetMediaQuery.NextAiringEpisode(episode = 10, airingAt = 1678886400)
                    )
                )
                val mockResponse = ApolloResponse.Builder(
                    operation,
                    Uuid(1, 1)
                ).data(expectedMedia).build()

                coEvery { mockApolloClient.executeQuery(operation, any()) } returns mockResponse

                val result = repository.getMedia(mediaId)

                result.isSuccess shouldBe true
                result.getOrNull() shouldBe AniListMedia(
                    id = mediaId,
                    format = "TV",
                    episodes = 12,
                    status = "RELEASING",
                    nextAiringEpisode = NextAiringEpisode(episode = 10, airingAt = 1678886400)
                )
            }
        }

//        `when`("APIエラーが発生した場合") {
//            then("Result.failureを返す") {
//                val mediaId = 123
//                val mockResponse = failure(
//                    operation = GetMediaQuery(id = mediaId),
//                    exception = RuntimeException("GraphQL Error")
//                )
//
//                coEvery { mockApolloClient.executeQuery(any(), any()) } returns mockResponse
//
//                val result = repository.getMedia(mediaId)
//
//                result.isFailure shouldBe true
//            }
//        }

//        `when`("mediaがnullの場合") {
//            then("Result.failureを返す") {
//                val mediaId = 123
//                val mockResponse = MockResponse(
//                    operation = GetMediaQuery(id = mediaId),
//                    data = GetMediaQuery.Data(null)
//                )
//
//                coEvery { mockApolloClient.executeQuery(any(), any()) } returns mockResponse
//
//                val result = repository.getMedia(mediaId)
//
//                result.isFailure shouldBe true
//            }
//        }
//
//        `when`("例外が発生した場合") {
//            then("Result.failureを返す") {
//                val mediaId = 123
//                coEvery { mockApolloClient.executeQuery(any(), any()) } throws RuntimeException("Network Error")
//
//                val result = repository.getMedia(mediaId)
//
//                result.isFailure shouldBe true
//            }
//        }
    }
})