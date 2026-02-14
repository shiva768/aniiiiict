package com.zelretch.aniiiiict.data.repository

import com.annict.CreateRecordMutation
import com.annict.DeleteRecordMutation
import com.annict.UpdateStatusMutation
import com.annict.ViewerProgramsQuery
import com.annict.ViewerRecordsQuery
import com.annict.type.StatusState
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Query
import com.benasher44.uuid.Uuid
import com.zelretch.aniiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiict.data.auth.TokenManager
import com.zelretch.aniiiiict.data.model.Record
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AnnictRepositoryImpl")
class AnnictRepositoryImplTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var authManager: AnnictAuthManager
    private lateinit var annictApolloClient: AnnictApolloClient
    private lateinit var repository: AnnictRepositoryImpl

    @BeforeEach
    fun setup() {
        tokenManager = mockk()
        authManager = mockk()
        annictApolloClient = mockk()
        repository = AnnictRepositoryImpl(tokenManager, authManager, annictApolloClient)
    }

    @Nested
    @DisplayName("isAuthenticatedメソッド")
    inner class IsAuthenticated {

        @Test
        @DisplayName("有効なトークンがある場合trueを返す")
        fun 有効なトークンがある場合trueを返す() = runTest {
            // Given
            coEvery { tokenManager.hasValidToken() } returns true

            // When
            val result = repository.isAuthenticated()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("有効なトークンがない場合falseを返す")
        fun 有効なトークンがない場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.hasValidToken() } returns false

            // When
            val result = repository.isAuthenticated()

            // Then
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("getAuthUrlメソッド")
    inner class GetAuthUrl {

        @Test
        @DisplayName("認証URL取得に成功した場合正しいURLを返す")
        fun 認証URL取得に成功した場合正しいURLを返す() = runTest {
            // Given
            val expectedUrl = "https://example.com/auth"
            coEvery { authManager.getAuthorizationUrl() } returns expectedUrl

            // When
            val result = repository.getAuthUrl()

            // Then
            assertEquals(expectedUrl, result)
        }

        @Test
        @DisplayName("認証URL取得に失敗した場合例外をスローする")
        fun 認証URL取得に失敗した場合例外をスローする() = runTest {
            // Given
            val expectedException = RuntimeException("Failed to get auth URL")
            coEvery { authManager.getAuthorizationUrl() } throws expectedException

            // When
            var caughtException: Exception? = null
            try {
                repository.getAuthUrl()
            } catch (e: Exception) {
                caughtException = e
            }

            // Then
            assertEquals(expectedException, caughtException)
        }
    }

    @Nested
    @DisplayName("createRecordメソッド")
    inner class CreateRecord {

        private val episodeId = "123"
        private val workId = "456"

        @Test
        @DisplayName("episodeIdが空の場合falseを返す")
        fun episodeIdが空の場合falseを返す() = runTest {
            // When
            val result = repository.createRecord("", workId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("workIdが空の場合falseを返す")
        fun workIdが空の場合falseを返す() = runTest {
            // When
            val result = repository.createRecord(episodeId, "")

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("アクセストークンがない場合falseを返す")
        fun アクセストークンがない場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns null

            // When
            val result = repository.createRecord(episodeId, workId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出し成功時にtrueを返す")
        fun API呼び出し成功時にtrueを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = CreateRecordMutation(episodeId = episodeId),
                requestUuid = Uuid(1, 1)
            ).data(CreateRecordMutation.Data(createRecord = null)).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.createRecord(episodeId, workId)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("API呼び出しがエラーを返す場合falseを返す")
        fun API呼び出しがエラーを返す場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = CreateRecordMutation(episodeId = episodeId),
                requestUuid = Uuid(1, 1)
            ).errors(listOf(Error.Builder(message = "API Error").build())).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.createRecord(episodeId, workId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出しが例外をスローする場合例外が伝播される")
        fun API呼び出しが例外をスローする場合例外が伝播される() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.createRecord(episodeId, workId)

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("handleAuthCallbackメソッド")
    inner class HandleAuthCallback {

        private val code = "test_code"

        @Test
        @DisplayName("認証成功時にtrueを返す")
        fun 認証成功時にtrueを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery { authManager.handleAuthorizationCode(code) } returns Result.success(Unit)

            // When
            val result = repository.handleAuthCallback(code)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("認証失敗時にfalseを返す")
        fun 認証失敗時にfalseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery { authManager.handleAuthorizationCode(code) } returns
                Result.failure(RuntimeException("Auth failed"))

            // When
            val result = repository.handleAuthCallback(code)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("認証中に例外が発生した場合falseを返す")
        fun 認証中に例外が発生した場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery { authManager.handleAuthorizationCode(code) } throws RuntimeException("Network error")

            // When
            val result = repository.handleAuthCallback(code)

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("getRawProgramsDataメソッド")
    inner class GetRawProgramsData {

        @Test
        @DisplayName("アクセストークンがない場合空のリストをemitする")
        fun アクセストークンがない場合空のリストをemitする() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns null

            // When
            val result = repository.getRawProgramsData()

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出し成功しデータがある場合ノードリストをemitする")
        fun API呼び出し成功しデータがある場合ノードリストをemitする() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = ViewerProgramsQuery(),
                requestUuid = Uuid(1, 1)
            ).data(
                ViewerProgramsQuery.Data(
                    viewer = ViewerProgramsQuery.Viewer(
                        programs = ViewerProgramsQuery.Programs(
                            ViewerProgramsQuery.PageInfo("", false),
                            listOf(mockk(relaxed = true))
                        )
                    )
                )
            ).build()
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.getRawProgramsData()

            // Then
            assertTrue(result.isSuccess)
            assertNotEquals(emptyList<ViewerProgramsQuery.Node?>(), result.getOrNull())
        }

        @Test
        @DisplayName("API呼び出しがエラーを返す場合空のリストをemitする")
        fun API呼び出しがエラーを返す場合空のリストをemitする() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = ViewerProgramsQuery(),
                requestUuid = Uuid(1, 1)
            ).errors(listOf(Error.Builder(message = "API Error").build())).build()
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.getRawProgramsData()

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出しが例外をスローする場合空のリストをemitする")
        fun API呼び出しが例外をスローする場合空のリストをemitする() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.getRawProgramsData()

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("コルーチンがキャンセルされた場合CancellationExceptionがスローされる")
        fun コルーチンがキャンセルされた場合CancellationExceptionがスローされる() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } throws CancellationException("Cancelled")

            // When
            var exceptionThrown = false
            try {
                repository.getRawProgramsData()
            } catch (e: CancellationException) {
                exceptionThrown = true
            }

            // Then
            assertTrue(exceptionThrown)
        }
    }

    @Nested
    @DisplayName("getRecordsメソッド")
    inner class GetRecords {

        @Test
        @DisplayName("アクセストークンがない場合空のPaginatedRecordsを返す")
        fun アクセストークンがない場合空のPaginatedRecordsを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns null

            // When
            val result = repository.getRecords()

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出し成功しデータがある場合変換されたPaginatedRecordsを返す")
        fun API呼び出し成功しデータがある場合変換されたPaginatedRecordsを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = ViewerRecordsQuery(),
                requestUuid = Uuid(1, 1)
            ).data(
                ViewerRecordsQuery.Data(
                    viewer = ViewerRecordsQuery.Viewer(
                        records = ViewerRecordsQuery.Records(
                            nodes = listOf(
                                ViewerRecordsQuery.Node(
                                    id = "123",
                                    comment = "",
                                    rating = 1.1,
                                    createdAt = "2020-01-01T00:00:00+09:00",
                                    episode = mockk<ViewerRecordsQuery.Episode>(
                                        relaxed = true
                                    )
                                )
                            ),
                            pageInfo = mockk<ViewerRecordsQuery.PageInfo>(relaxed = true)
                        )
                    )
                )
            ).build()
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.getRecords()

            // Then
            assertTrue(result.isSuccess)
            assertNotEquals(emptyList<Record>(), result.getOrNull()!!.records)
        }

        @Test
        @DisplayName("API呼び出しがエラーを返す場合空のPaginatedRecordsを返す")
        fun API呼び出しがエラーを返す場合空のPaginatedRecordsを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = ViewerRecordsQuery(),
                requestUuid = Uuid(1, 1)
            ).errors(listOf(Error.Builder(message = "API Error").build())).build()
            coEvery {
                annictApolloClient.executeQuery(
                    any<ViewerRecordsQuery>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.getRecords()

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出しが例外をスローする場合空のPaginatedRecordsを返す")
        fun API呼び出しが例外をスローする場合空のPaginatedRecordsを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeQuery(
                    any<Query<*>>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.getRecords()

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("deleteRecordメソッド")
    inner class DeleteRecord {

        private val recordId = "record123"

        @Test
        @DisplayName("アクセストークンがない場合falseを返す")
        fun アクセストークンがない場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns null

            // When
            val result = repository.deleteRecord(recordId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出し成功時にtrueを返す")
        fun API呼び出し成功時にtrueを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = DeleteRecordMutation(recordId = recordId),
                requestUuid = Uuid(1, 1)
            ).data(DeleteRecordMutation.Data(deleteRecord = null)).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.deleteRecord(recordId)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("API呼び出しがエラーを返す場合falseを返す")
        fun API呼び出しがエラーを返す場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = DeleteRecordMutation(recordId = recordId),
                requestUuid = Uuid(1, 1)
            ).errors(listOf(Error.Builder(message = "API Error").build())).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.deleteRecord(recordId)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出しが例外をスローする場合falseを返す")
        fun API呼び出しが例外をスローする場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.deleteRecord(recordId)

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("updateWorkViewStatusメソッド")
    inner class UpdateWorkViewStatus {

        private val workId = "work123"
        private val status = StatusState.WATCHING

        @Test
        @DisplayName("アクセストークンがない場合falseを返す")
        fun アクセストークンがない場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns null

            // When
            val result = repository.updateWorkViewStatus(workId, status)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出し成功時にtrueを返す")
        fun API呼び出し成功時にtrueを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = UpdateStatusMutation(workId = workId, state = status),
                requestUuid = Uuid(1, 1)
            ).data(UpdateStatusMutation.Data(updateStatus = null)).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.updateWorkViewStatus(workId, status)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("API呼び出しがエラーを返す場合falseを返す")
        fun API呼び出しがエラーを返す場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            val mockResponse = ApolloResponse.Builder(
                operation = UpdateStatusMutation(workId = workId, state = status),
                requestUuid = Uuid(1, 1)
            ).errors(listOf(Error.Builder(message = "API Error").build())).build()
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } returns mockResponse

            // When
            val result = repository.updateWorkViewStatus(workId, status)

            // Then
            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("API呼び出しが例外をスローする場合falseを返す")
        fun API呼び出しが例外をスローする場合falseを返す() = runTest {
            // Given
            coEvery { tokenManager.getAccessToken() } returns "token"
            coEvery {
                annictApolloClient.executeMutation(
                    any<Mutation<*>>(),
                    any<String>()
                )
            } throws RuntimeException("Network Error")

            // When
            val result = repository.updateWorkViewStatus(workId, status)

            // Then
            assertTrue(result.isFailure)
        }
    }
}
