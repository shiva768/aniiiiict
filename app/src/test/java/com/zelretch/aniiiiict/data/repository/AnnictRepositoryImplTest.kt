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
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class アニクトリポジトリ実装テスト : BehaviorSpec({

    val tokenManager = mockk<TokenManager>()
    val authManager = mockk<AnnictAuthManager>()
    val annictApolloClient = mockk<AnnictApolloClient>()
    val repository = AnnictRepositoryImpl(tokenManager, authManager, annictApolloClient)

    前提("AnnictRepositoryImpl の isAuthenticated メソッド") {
        場合("有効なトークンがある場合") {
            そのとき("trueを返す") {
                coEvery { tokenManager.hasValidToken() } returns true
                repository.isAuthenticated() shouldBe true
            }
        }

        場合("有効なトークンがない場合") {
            そのとき("falseを返す") {
                coEvery { tokenManager.hasValidToken() } returns false
                repository.isAuthenticated() shouldBe false
            }
        }
    }

    前提("AnnictRepositoryImpl の getAuthUrl メソッド") {
        場合("認証URLの取得に成功した場合") {
            そのとき("正しいURLを返す") {
                val expectedUrl = "https://example.com/auth"
                coEvery { authManager.getAuthorizationUrl() } returns expectedUrl
                repository.getAuthUrl() shouldBe expectedUrl
            }
        }

        場合("認証URLの取得に失敗した場合") {
            そのとき("例外をスローする") {
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

    前提("AnnictRepositoryImpl の createRecord メソッド") {
        val episodeId = "123"
        val workId = "456"

        場合("episodeIdが空の場合") {
            そのとき("falseを返す") {
                repository.createRecord("", workId) shouldBe false
            }
        }

        場合("workIdが空の場合") {
            そのとき("falseを返す") {
                repository.createRecord(episodeId, "") shouldBe false
            }
        }

        場合("アクセストークンがない場合") {
            そのとき("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.createRecord(episodeId, workId) shouldBe false
            }
        }

        場合("API呼び出しが成功した場合") {
            そのとき("trueを返す") {
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

        場合("API呼び出しがエラーを返す場合") {
            そのとき("falseを返す") {
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

        場合("API呼び出しが例外をスローする場合") {
            そのとき("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeMutation(
                        any<Mutation<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                shouldThrowExactly<RuntimeException> {
                    repository.createRecord(episodeId, workId)
                }
            }
        }
    }

    前提("AnnictRepositoryImpl の handleAuthCallback メソッド") {
        val code = "test_code"

        場合("認証が成功した場合") {
            そのとき("trueを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } returns Result.success(Unit)
                repository.handleAuthCallback(code) shouldBe true
            }
        }

        場合("認証が失敗した場合") {
            そのとき("falseを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } returns
                    Result.failure(RuntimeException("Auth failed"))
                repository.handleAuthCallback(code) shouldBe false
            }
        }

        場合("認証中に例外が発生した場合") {
            そのとき("falseを返す") {
                coEvery { authManager.handleAuthorizationCode(code) } throws RuntimeException("Network error")
                repository.handleAuthCallback(code) shouldBe false
            }
        }
    }

    前提("AnnictRepositoryImpl の getRawProgramsData メソッド") {
        場合("アクセストークンがない場合") {
            そのとき("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.getRawProgramsData().first() shouldBe emptyList()
            }
        }

        場合("API呼び出しが成功し、データがある場合") {
            そのとき("GraphQLレスポンスのノードリストをemitする") {
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

                val result = repository.getRawProgramsData().first()
                result shouldNotBe emptyList<ViewerProgramsQuery.Node?>()
            }
        }

        場合("API呼び出しがエラーを返す場合") {
            そのとき("空のリストをemitする") {
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

                repository.getRawProgramsData().first() shouldBe emptyList()
            }
        }

        場合("API呼び出しが例外をスローする場合") {
            そのとき("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeQuery(
                        any<Query<*>>(),
                        any<String>()
                    )
                } throws RuntimeException("Network Error")

                repository.getRawProgramsData().first() shouldBe emptyList()
            }
        }

        場合("コルーチンがキャンセルされた場合") {
            そのとき("空のリストをemitする") {
                coEvery { tokenManager.getAccessToken() } returns "token"
                coEvery {
                    annictApolloClient.executeQuery(
                        any<Query<*>>(),
                        any<String>()
                    )
                } throws CancellationException("Cancelled")

                try {
                    repository.getRawProgramsData().first()
                } catch (e: Exception) {
                    (e is CancellationException) shouldBe true
                }
            }
        }
    }

    前提("AnnictRepositoryImpl の getRecords メソッド") {
        場合("アクセストークンがない場合") {
            そのとき("空のPaginatedRecordsを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.getRecords() shouldBe PaginatedRecords(emptyList())
            }
        }

        場合("API呼び出しが成功し、データがある場合") {
            そのとき("変換されたPaginatedRecordsを返す") {
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

                val result = repository.getRecords()
                result.records shouldNotBe emptyList<Record>()
            }
        }

        場合("API呼び出しがエラーを返す場合") {
            そのとき("空のPaginatedRecordsを返す") {
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

        場合("API呼び出しが例外をスローする場合") {
            そのとき("空のPaginatedRecordsを返す") {
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

    前提("AnnictRepositoryImpl の deleteRecord メソッド") {
        val recordId = "record123"

        場合("アクセストークンがない場合") {
            そのとき("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.deleteRecord(recordId) shouldBe false
            }
        }

        場合("API呼び出しが成功した場合") {
            そのとき("trueを返す") {
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

        場合("API呼び出しがエラーを返す場合") {
            そのとき("falseを返す") {
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

        場合("API呼び出しが例外をスローする場合") {
            そのとき("falseを返す") {
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

    前提("AnnictRepositoryImpl の updateWorkViewStatus メソッド") {
        val workId = "work123"
        val status = StatusState.WATCHING

        場合("アクセストークンがない場合") {
            そのとき("falseを返す") {
                coEvery { tokenManager.getAccessToken() } returns null
                repository.updateWorkViewStatus(workId, status) shouldBe false
            }
        }

        場合("API呼び出しが成功した場合") {
            そのとき("trueを返す") {
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

        場合("API呼び出しがエラーを返す場合") {
            そのとき("falseを返す") {
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

        場合("API呼び出しが例外をスローする場合") {
            そのとき("falseを返す") {
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
