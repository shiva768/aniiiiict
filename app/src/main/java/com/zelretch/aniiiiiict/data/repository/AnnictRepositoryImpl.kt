package com.zelretch.aniiiiiict.data.repository

import com.annict.CreateRecordMutation
import com.annict.DeleteRecordMutation
import com.annict.UpdateStatusMutation
import com.annict.ViewerProgramsQuery
import com.annict.ViewerRecordsQuery
import com.annict.type.StatusState
import com.apollographql.apollo.api.Optional
import com.zelretch.aniiiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.util.Logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val annictApolloClient: AnnictApolloClient,
    private val logger: Logger
) : AnnictRepository {
    override suspend fun isAuthenticated(): Boolean {
        val result = tokenManager.hasValidToken()
        logger.info("AnnictRepositoryImpl", "認証状態 = $result", "AnnictRepositoryImpl.isAuthenticated")
        return result
    }

    override suspend fun getAuthUrl(): String = authManager.getAuthorizationUrl()

    override suspend fun createRecord(episodeId: String, workId: String): Boolean {
        // パラメータバリデーション（APIリクエスト前に行う必要あり）
        if (episodeId.isEmpty() || workId.isEmpty()) {
            logger.error(
                TAG,
                "エピソードIDまたは作品IDがnullまたは空です",
                "AnnictRepositoryImpl.createRecord"
            )
            return false
        }

        return executeApiRequest(
            operation = "createRecord",
            defaultValue = false
        ) {
            logger.info(
                TAG,
                "エピソード記録を実行: episodeId=$episodeId, workId=$workId",
                "AnnictRepositoryImpl.createRecord"
            )

            val mutation = CreateRecordMutation(episodeId = episodeId)
            val response = annictApolloClient.executeMutation(
                operation = mutation,
                context = "AnnictRepositoryImpl.createRecord"
            )

            logger.info(
                TAG,
                "GraphQLのレスポンス: ${response.data != null}, エラー: ${response.errors}",
                "AnnictRepositoryImpl.createRecord"
            )

            !response.hasErrors()
        }
    }

    override suspend fun handleAuthCallback(code: String): Boolean {
        logger.info(
            TAG,
            "認証コールバック処理開始 - コード: ${code.take(5)}...",
            "AnnictRepositoryImpl.handleAuthCallback"
        )
        return try {
            authManager.handleAuthorizationCode(code).fold(onSuccess = {
                logger.info(TAG, "認証成功", "AnnictRepositoryImpl.handleAuthCallback")
                true
            }, onFailure = { e ->
                logger.error(
                    TAG,
                    "認証失敗 - ${e.message}",
                    "AnnictRepositoryImpl.handleAuthCallback"
                )
                false
            })
        } catch (e: Exception) {
            logger.error(
                TAG,
                "認証処理中に例外が発生 - ${e.message}",
                "AnnictRepositoryImpl.handleAuthCallback"
            )
            false
        }
    }

    override suspend fun getRawProgramsData(): Flow<List<ViewerProgramsQuery.Node?>> {
        return flow {
            try {
                logger.info(
                    TAG,
                    "プログラム一覧の取得を開始",
                    "AnnictRepositoryImpl.getRawProgramsData"
                )

                // キャンセルされた場合は例外をスローせずに空のリストを返す
                if (!currentCoroutineContext().isActive) {
                    logger.info(
                        TAG,
                        "処理がキャンセルされたため、実行をスキップします",
                        "AnnictRepositoryImpl.getRawProgramsData"
                    )
                    emit(emptyList())
                    return@flow
                }

                // アクセストークンの確認
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    logger.error(
                        TAG,
                        "アクセストークンがありません",
                        "AnnictRepositoryImpl.getRawProgramsData"
                    )
                    emit(emptyList())
                    return@flow
                }

                val query = ViewerProgramsQuery()
                val response = annictApolloClient.executeQuery(
                    operation = query,
                    context = "AnnictRepositoryImpl.getRawProgramsData"
                )

                logger.info(
                    TAG,
                    "GraphQLのレスポンス: ${response.data != null}",
                    "AnnictRepositoryImpl.getRawProgramsData"
                )

                if (response.hasErrors()) {
                    logger.error(
                        TAG,
                        "GraphQLエラー: ${response.errors}",
                        "AnnictRepositoryImpl.getRawProgramsData"
                    )
                    emit(emptyList())
                    return@flow
                }

                val programs = response.data?.viewer?.programs?.nodes
                logger.info(
                    TAG,
                    "取得したプログラム数: ${programs?.size ?: 0}",
                    "AnnictRepositoryImpl.getRawProgramsData"
                )

                emit(programs ?: emptyList())
            } catch (e: Exception) {
                // キャンセル例外の場合は再スローして上位で処理
                if (e is kotlinx.coroutines.CancellationException) throw e

                logger.error(TAG, e, "プログラム一覧の取得に失敗")
                emit(emptyList())
            }
        }
    }

    override suspend fun getRecords(after: String?): PaginatedRecords = executeApiRequest(
        operation = "getRecords",
        defaultValue = PaginatedRecords(emptyList())
    ) {
        logger.info(TAG, "記録履歴を取得中...", "AnnictRepositoryImpl.getRecords")

        val query = ViewerRecordsQuery(
            after =
            after?.let { Optional.present(it) } ?: Optional.absent()
        )
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

            logger.info(
                TAG,
                "${records.size}件の記録を取得しました",
                "AnnictRepositoryImpl.getRecords"
            )
            PaginatedRecords(
                records = records,
                hasNextPage = pageInfo?.hasNextPage == true,
                endCursor = pageInfo?.endCursor
            )
        } else {
            logger.error(
                TAG,
                "GraphQLエラー: ${response.errors}",
                "AnnictRepositoryImpl.getRecords"
            )
            PaginatedRecords(emptyList())
        }
    }

    override suspend fun deleteRecord(recordId: String): Boolean = executeApiRequest(
        operation = "deleteRecord",
        defaultValue = false
    ) {
        logger.info(
            TAG,
            "記録を削除中: $recordId",
            "AnnictRepositoryImpl.deleteRecord"
        )

        val mutation = DeleteRecordMutation(recordId)
        val response = annictApolloClient.executeMutation(
            operation = mutation,
            context = "AnnictRepositoryImpl.deleteRecord"
        )

        if (!response.hasErrors()) {
            logger.info(
                TAG,
                "記録を削除しました: $recordId",
                "AnnictRepositoryImpl.deleteRecord"
            )
            true
        } else {
            logger.error(
                TAG,
                "GraphQLエラー: ${response.errors}",
                "AnnictRepositoryImpl.deleteRecord"
            )
            false
        }
    }

    override suspend fun updateWorkViewStatus(workId: String, state: StatusState): Boolean =
        executeApiRequest(
            operation = "updateWorkStatus",
            defaultValue = false
        ) {
            logger.info(
                TAG,
                "作品ステータスを更新中: workId=$workId, state=$state",
                "AnnictRepositoryImpl.updateWorkStatus"
            )

            val mutation = UpdateStatusMutation(workId = workId, state = state)
            val response = annictApolloClient.executeMutation(
                operation = mutation,
                context = "AnnictRepositoryImpl.updateWorkStatus"
            )

            if (!response.hasErrors()) {
                logger.info(
                    TAG,
                    "作品ステータスを更新しました: workId=$workId, state=$state",
                    "AnnictRepositoryImpl.updateWorkStatus"
                )
                true
            } else {
                logger.error(
                    TAG,
                    "GraphQLエラー: ${response.errors}",
                    "AnnictRepositoryImpl.updateWorkStatus"
                )
                false
            }
        }

    // 共通のAPIリクエスト処理を行うヘルパーメソッド
    private suspend fun <T> executeApiRequest(
        operation: String,
        defaultValue: T,
        request: suspend () -> T
    ): T {
        if (!currentCoroutineContext().isActive) {
            logger.info(
                TAG,
                "処理がキャンセルされたため、実行をスキップします",
                "AnnictRepositoryImpl.$operation"
            )
            return defaultValue
        }

        val token = tokenManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            logger.error(
                TAG,
                "アクセストークンがありません",
                "AnnictRepositoryImpl.$operation"
            )
            return defaultValue
        }

        return try {
            request()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error(TAG, e, "AnnictRepositoryImpl.$operation")
            defaultValue
        }
    }
}
