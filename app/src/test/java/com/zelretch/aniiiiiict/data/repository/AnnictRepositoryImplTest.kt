package com.zelretch.aniiiiiict.data.repository

import com.annict.*
import com.annict.type.StatusState
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Query
import com.benasher44.uuid.Uuid
import com.zelretch.aniiiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.util.TestLogger
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class AnnictRepositoryImplTest : BehaviorSpec({

    val tokenManager = mockk<TokenManager>()
    val authManager = mockk<AnnictAuthManager>()
    val annictApolloClient = mockk<AnnictApolloClient>()
    val logger = TestLogger()
    val repository = AnnictRepositoryImpl(tokenManager, authManager, annictApolloClient, logger)

    given("AnnictRepositoryImpl の isAuthenticated メソッド") {
        `when`("有効なトークンがある場合") {
            then("trueを返す") {
                coEvery { tokenManager.hasValidToken() } returns true
                repository.isAuthenticated() shouldBe true
            }
        }

        `when`("有効なトークンがない場合") {
            then("falseを返す") {
                coEvery { tokenManager.hasValidToken() } returns false
                repository.isAuthenticated() shouldBe false
            }
        }
    }

    given("AnnictRepositoryImpl の getAuthUrl メソッド") {
        `when`("認証URLの取得に成功した場合") {
            then("正しいURLを返す") {
                val expectedUrl = "https://example.com/auth"
                coEvery { authManager.getAuthorizationUrl() } returns expectedUrl
                repository.getAuthUrl() shouldBe expectedUrl
            }
        }

        `when`("認証URLの取得に失敗した場合") {
            then("例外をスローする") {
                val expectedException = RuntimeException("Failed to get auth URL")
                coEvery { authManager.getAuthorizationUrl() } throws expectedException
                try {
                    repository.getAuthUrl()
                } catch (e: Exception) {
                    e shouldBe expectedException
                }
            }
        }
    }

    given("AnnictRepositoryImpl の createRecord メソッド") {
        val episodeId = "123"
        val workId = "456"

        `when`("episodeIdが空の場合") {
            then("falseを返す") {
                repository.createRecord("", workId) shouldBe false
            }
        }

        `when`("workIdが空の場合") {
            then("falseを返す") {
                repository.createRecord(episodeId, "") shouldBe false
            }
        }

        `when`("アクセストークンがない場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.createRecord(episodeId, workId) shouldBe false
            }
        }

        `when`("API呼び出しが成功した場合") {
            then("trueを返す") {
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

                repository.createRecord(episodeId, workId) shouldBe true
            }
        }

        `when`("API呼び出しがエラーを返す場合") {
            then("falseを返す") {
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

                repository.createRecord(episodeId, workId) shouldBe false
            }
        }

        `when`("API呼び出しが例外をスローする場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeMutation(
                        any<Mutation<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.createRecord(episodeId, workId) shouldBe false
            }
        }
    }

    given("AnnictRepositoryImpl の handleAuthCallback メソッド") {
        val code = "test_code"

        `when`("認証が成功した場合") {
            then("trueを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } returns Result.success(Unit)
                repository.handleAuthCallback(code) shouldBe true
            }
        }

        `when`("認証が失敗した場合") {
            then("falseを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } returns Result.failure(RuntimeException("Auth failed"))
                repository.handleAuthCallback(code) shouldBe false
            }
        }

        `when`("認証中に例外が発生した場合") {
            then("falseを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } throws RuntimeException("Network error")
                repository.handleAuthCallback(code) shouldBe false
            }
        }
    }

    given("AnnictRepositoryImpl の getProgramsWithWorks メソッド") {
        `when`("アクセストークンがない場合") {
            then("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.getProgramsWithWorks().first() shouldBe emptyList()
            }
        }

        `when`("API呼び出しが成功し、データがある場合") {
            then("変換されたProgramWithWorkのリストをemitする") {
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

                val result = repository.getProgramsWithWorks().first()
                result shouldNotBe emptyList<ProgramWithWork>()
            }
        }

        `when`("API呼び出しがエラーを返す場合") {
            then("空のリストをemitする") {
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

                repository.getProgramsWithWorks().first() shouldBe emptyList()
            }
        }

        `when`("API呼び出しが例外をスローする場合") {
            then("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeQuery(
                        any<Query<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.getProgramsWithWorks().first() shouldBe emptyList()
            }
        }

        `when`("コルーチンがキャンセルされた場合") {
            then("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeQuery(
                        any<Query<*>>(),
                        any<String>()
                    )
                } throws CancellationException("Cancelled")

                try {
                    repository.getProgramsWithWorks().first()
                } catch (e: Exception) {
                    (e is CancellationException) shouldBe true
                }
            }
        }
    }

    given("AnnictRepositoryImpl の getRecords メソッド") {
        "cursor123"

        `when`("アクセストークンがない場合") {
            then("空のPaginatedRecordsを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.getRecords() shouldBe PaginatedRecords(emptyList())
            }
        }

        `when`("API呼び出しが成功し、データがある場合") {
            then("変換されたPaginatedRecordsを返す") {
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
                                        episode = mockk<ViewerRecordsQuery.Episode>(relaxed = true),
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

                val result = repository.getRecords()
                result.records shouldNotBe emptyList<Record>()
            }
        }

        `when`("API呼び出しがエラーを返す場合") {
            then("空のPaginatedRecordsを返す") {
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

                repository.getRecords() shouldBe PaginatedRecords(emptyList())
            }
        }

        `when`("API呼び出しが例外をスローする場合") {
            then("空のPaginatedRecordsを返す") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeQuery(
                        any<Query<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.getRecords() shouldBe PaginatedRecords(emptyList())
            }
        }
    }

    given("AnnictRepositoryImpl の deleteRecord メソッド") {
        val recordId = "record123"

        `when`("アクセストークンがない場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.deleteRecord(recordId) shouldBe false
            }
        }

        `when`("API呼び出しが成功した場合") {
            then("trueを返す") {
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

                repository.deleteRecord(recordId) shouldBe true
            }
        }

        `when`("API呼び出しがエラーを返す場合") {
            then("falseを返す") {
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

                repository.deleteRecord(recordId) shouldBe false
            }
        }

        `when`("API呼び出しが例外をスローする場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeMutation(
                        any<Mutation<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.deleteRecord(recordId) shouldBe false
            }
        }
    }

    given("AnnictRepositoryImpl の updateWorkViewStatus メソッド") {
        val workId = "work123"
        val status = StatusState.WATCHING

        `when`("アクセストークンがない場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.updateWorkViewStatus(workId, status) shouldBe false
            }
        }

        `when`("API呼び出しが成功した場合") {
            then("trueを返す") {
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

                repository.updateWorkViewStatus(workId, status) shouldBe true
            }
        }

        `when`("API呼び出しがエラーを返す場合") {
            then("falseを返す") {
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

                repository.updateWorkViewStatus(workId, status) shouldBe false
            }
        }

        `when`("API呼び出しが例外をスローする場合") {
            then("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeMutation(
                        any<Mutation<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.updateWorkViewStatus(workId, status) shouldBe false
            }
        }
    }
})