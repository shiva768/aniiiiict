package com.zelretch.aniiiiict.data.repository

import co.anilist.GetMediaQuery
import co.anilist.type.MediaFormat
import co.anilist.type.MediaStatus
import com.apollographql.apollo.api.ApolloResponse
import com.benasher44.uuid.Uuid
import com.zelretch.aniiiiict.data.api.AniListApolloClient
import com.zelretch.aniiiiict.data.model.AniListMedia
import com.zelretch.aniiiiict.data.model.NextAiringEpisode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AniListRepositoryImpl")
class AniListRepositoryImplTest {

    private lateinit var mockApolloClient: AniListApolloClient
    private lateinit var repository: AniListRepositoryImpl

    @BeforeEach
    fun setup() {
        mockApolloClient = mockk()
        repository = AniListRepositoryImpl(mockApolloClient)
    }

    @Nested
    @DisplayName("getMediaメソッド")
    inner class GetMedia {

        @Test
        @DisplayName("API呼び出し成功時にAniListMediaを返す")
        fun API呼び出し成功時にAniListMediaを返す() = runTest {
            // Given
            val mediaId = 123
            val operation = GetMediaQuery(mediaId)
            val expectedMedia = GetMediaQuery.Data(
                GetMediaQuery.Media(
                    id = mediaId,
                    format = MediaFormat.TV,
                    episodes = 12,
                    status = MediaStatus.RELEASING,
                    nextAiringEpisode = GetMediaQuery.NextAiringEpisode(
                        episode = 10,
                        airingAt = 1678886400
                    )
                )
            )
            val mockResponse = ApolloResponse.Builder(
                operation = operation,
                requestUuid = Uuid(1, 1)
            ).data(expectedMedia).build()

            coEvery { mockApolloClient.executeQuery(operation, any<String>()) } returns mockResponse

            // When
            val result = repository.getMedia(mediaId)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(
                AniListMedia(
                    id = mediaId,
                    format = "TV",
                    episodes = 12,
                    status = "RELEASING",
                    nextAiringEpisode = NextAiringEpisode(episode = 10, airingAt = 1678886400)
                ),
                result.getOrNull()
            )
        }

        @Test
        @DisplayName("APIエラー発生時にResult.failureを返す")
        fun APIエラー発生時にResultFailureを返す() = runTest {
            // Given
            val mediaId = 123
            val mockResponse = ApolloResponse.Builder(
                operation = GetMediaQuery(id = mediaId),
                requestUuid = Uuid(1, 1)
            ).errors(
                listOf(
                    com.apollographql.apollo.api.Error.Builder(
                        message = "API Error"
                    ).build()
                )
            ).build()

            coEvery {
                mockApolloClient.executeQuery(any<GetMediaQuery>(), any<String>())
            } returns mockResponse

            // When
            val result = repository.getMedia(mediaId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("mediaがnullの場合Result.failureを返す")
        fun mediaがnullの場合ResultFailureを返す() = runTest {
            // Given
            val mediaId = 123
            val mockResponse = ApolloResponse.Builder(
                operation = GetMediaQuery(id = mediaId),
                requestUuid = Uuid(1, 1)
            ).data(GetMediaQuery.Data(null)).build()

            coEvery {
                mockApolloClient.executeQuery(any<GetMediaQuery>(), any<String>())
            } returns mockResponse

            // When
            val result = repository.getMedia(mediaId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("例外発生時にResult.failureを返す")
        fun 例外発生時にResultFailureを返す() = runTest {
            // Given
            val mediaId = 123
            coEvery {
                mockApolloClient.executeQuery(
                    any<GetMediaQuery>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.getMedia(mediaId)

            // Then
            assertTrue(result.isFailure)
        }
    }
}
