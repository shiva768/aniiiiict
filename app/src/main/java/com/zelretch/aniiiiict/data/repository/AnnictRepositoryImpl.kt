package com.zelretch.aniiiiict.data.repository

import com.annict.CreateRecordMutation
import com.annict.DeleteRecordMutation
import com.annict.UpdateStatusMutation
import com.annict.ViewerProgramsQuery
import com.annict.ViewerRecordsQuery
import com.annict.type.StatusState
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import com.zelretch.aniiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiict.data.auth.TokenManager
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.io.IOException
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val annictApolloClient: AnnictApolloClient
) : AnnictRepository {

    companion object {
        private const val AUTH_CODE_LOG_LENGTH = 5
    }

    override suspend fun isAuthenticated(): Boolean {
        val result = tokenManager.hasValidToken()
        Timber.i("認証状態 = $result")
        return result
    }

    override suspend fun getAuthUrl(): String = authManager.getAuthorizationUrl()

    override suspend fun createRecord(episodeId: String, workId: String): Boolean {
        // パラメータバリデーション（APIリクエスト前に行う必要あり）
        if (episodeId.isEmpty() || workId.isEmpty()) {
            Timber.e("エピソードIDまたは作品IDがnullまたは空です")
            return false
        }

        // executeApiRequest を使わず、予期しない例外は上位へスローしてテスト期待に合わせる
        val token = tokenManager.getAccessToken()
        if (!currentCoroutineContext().isActive || token.isNullOrEmpty()) {
            Timber.w("リクエスト実行の前提条件未達、または処理キャンセル: operation=createRecord, tokenIsEmpty=${token.isNullOrEmpty()}")
            return false
        }

        return try {
            Timber.i(
                "エピソード記録を実行: episodeId=$episodeId, workId=$workId"
            )

            val mutation = CreateRecordMutation(episodeId = episodeId)
            val response = annictApolloClient.executeMutation(
                operation = mutation,
                context = "AnnictRepositoryImpl.createRecord"
            )

            Timber.i(
                "GraphQLのレスポンス: ${response.data != null}, エラー: ${response.errors}"
            )

            !response.hasErrors()
        } catch (e: ApolloException) {
            Timber.e(e, "[AnnictRepositoryImpl][createRecord] API error while creating record")
            false
        } catch (e: IOException) {
            Timber.e(e, "[AnnictRepositoryImpl][createRecord] Network IO error while creating record")
            false
        }
    }

    override suspend fun handleAuthCallback(code: String): Boolean {
        Timber.i(
            "認証コールバック処理開始 - コード: ${code.take(AUTH_CODE_LOG_LENGTH)}..."
        )
        return try {
            authManager.handleAuthorizationCode(code).fold(onSuccess = {
                Timber.i("認証成功")
                true
            }, onFailure = { e ->
                Timber.e(
                    e,
                    "認証失敗"
                )
                false
            })
        } catch (e: ApolloException) {
            Timber.e(e, "[AnnictRepositoryImpl][exchangeCodeForToken] API error while handling auth callback")
            false
        } catch (e: IOException) {
            Timber.e(e, "[AnnictRepositoryImpl][exchangeCodeForToken] Network IO error while handling auth callback")
            false
        } catch (e: Exception) {
            Timber.e(e, "[AnnictRepositoryImpl][exchangeCodeForToken] Unexpected error while handling auth callback")
            false
        }
    }

    override suspend fun getRawProgramsData(): Flow<List<ViewerProgramsQuery.Node?>> {
        return flow {
            try {
                Timber.i("プログラム一覧の取得を開始")

                // キャンセルされた場合は例外をスローせずに空のリストを返す
                if (!currentCoroutineContext().isActive) {
                    Timber.i(
                        "処理がキャンセルされたため、実行をスキップします"
                    )
                    emit(emptyList())
                    return@flow
                }

                // アクセストークンの確認
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    Timber.e(
                        "アクセストークンがありません"
                    )
                    emit(emptyList())
                    return@flow
                }

                val query = ViewerProgramsQuery()
                val response = annictApolloClient.executeQuery(
                    operation = query,
                    context = "AnnictRepositoryImpl.getRawProgramsData"
                )

                Timber.i(
                    "GraphQLのレスポンス: ${response.data != null}"
                )

                if (response.hasErrors()) {
                    Timber.e(
                        "GraphQLエラー: ${response.errors}"
                    )
                    emit(emptyList())
                    return@flow
                }

                val programs = response.data?.viewer?.programs?.nodes
                Timber.i(
                    "取得したプログラム数: ${programs?.size ?: 0}"
                )

                emit(programs ?: emptyList())
            } catch (e: ApolloException) {
                Timber.e(e, "[AnnictRepositoryImpl][getRawProgramsData] API error during fetching programs")
                emit(emptyList())
            } catch (e: IOException) {
                Timber.e(e, "[AnnictRepositoryImpl][getRawProgramsData] Network IO error during fetching programs")
                emit(emptyList())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "[AnnictRepositoryImpl][getRawProgramsData] Unexpected error during fetching programs")
                emit(emptyList())
            }
        }
    }

    override suspend fun getRecords(after: String?): PaginatedRecords = executeApiRequest(
        operation = "getRecords",
        defaultValue = PaginatedRecords(emptyList())
    ) {
        Timber.i("記録履歴を取得中...")

        val query = ViewerRecordsQuery(after = after?.let { Optional.present(it) } ?: Optional.absent())
        val response = annictApolloClient.executeQuery(
            operation = query,
            context = "AnnictRepositoryImpl.getRecords"
        )

        if (!response.hasErrors()) {
            val nodes = response.data?.viewer?.records?.nodes ?: emptyList()
            val pageInfo = response.data?.viewer?.records?.pageInfo
            val records = nodes.mapNotNull { node ->
                node?.let {
                    val episode = it.episode
                    val work = episode.work

                    Record(
                        id = it.id,
                        comment = it.comment,
                        rating = it.rating,
                        createdAt = ZonedDateTime.parse(it.createdAt.toString()),
                        episode = Episode(
                            id = episode.id,
                            number = null,
                            numberText = episode.numberText ?: "",
                            title = episode.title ?: "",
                            viewerDidTrack = episode.viewerDidTrack
                        ),
                        work = Work(
                            id = work.id,
                            title = work.title,
                            media = null,
                            mediaText = "",
                            viewerStatusState = StatusState.UNKNOWN__,
                            seasonNameText = ""
                        )
                    )
                }
            }

            Timber.i(
                "${records.size}件の記録を取得しました"
            )
            PaginatedRecords(
                records = records,
                hasNextPage = pageInfo?.hasNextPage == true,
                endCursor = pageInfo?.endCursor
            )
        } else {
            Timber.e(
                "GraphQLエラー: ${response.errors}"
            )
            PaginatedRecords(emptyList())
        }
    }

    override suspend fun deleteRecord(recordId: String): Boolean = executeApiRequest(
        operation = "deleteRecord",
        defaultValue = false
    ) {
        Timber.i(
            "記録を削除中: $recordId"
        )

        val mutation = DeleteRecordMutation(recordId)
        val response = annictApolloClient.executeMutation(
            operation = mutation,
            context = "AnnictRepositoryImpl.deleteRecord"
        )

        if (!response.hasErrors()) {
            Timber.i(
                "記録を削除しました: $recordId"
            )
            true
        } else {
            Timber.e(
                "GraphQLエラー: ${response.errors}"
            )
            false
        }
    }

    override suspend fun updateWorkViewStatus(workId: String, state: StatusState): Boolean = executeApiRequest(
        operation = "updateWorkStatus",
        defaultValue = false
    ) {
        Timber.i(
            "作品ステータスを更新中: workId=$workId, state=$state"
        )

        val mutation = UpdateStatusMutation(workId = workId, state = state)
        val response = annictApolloClient.executeMutation(
            operation = mutation,
            context = "AnnictRepositoryImpl.updateWorkStatus"
        )

        if (!response.hasErrors()) {
            Timber.i(
                "作品ステータスを更新しました: workId=$workId, state=$state"
            )
            true
        } else {
            Timber.e(
                "GraphQLエラー: ${response.errors}"
            )
            false
        }
    }

    // 共通のAPIリクエスト処理を行うヘルパーメソッド
    private suspend fun <T> executeApiRequest(operation: String, defaultValue: T, request: suspend () -> T): T {
        val token = tokenManager.getAccessToken()
        if (!currentCoroutineContext().isActive || token.isNullOrEmpty()) {
            Timber.w("リクエスト実行の前提条件未達、または処理キャンセル: operation=$operation, tokenIsEmpty=${token.isNullOrEmpty()}")
            return defaultValue
        }

        return try {
            request()
        } catch (e: ApolloException) {
            Timber.e(e, "[AnnictRepositoryImpl][$operation] API error during request")
            defaultValue
        } catch (e: IOException) {
            Timber.e(e, "[AnnictRepositoryImpl][$operation] Network IO error during request")
            defaultValue
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "[AnnictRepositoryImpl][$operation] Unexpected error during request")
            defaultValue
        }
    }
}
