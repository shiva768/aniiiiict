package com.zelretch.aniiiiiict.data.repository

import com.apollographql.apollo.api.Optional
import com.zelretch.aniiiiiict.*
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.model.*
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.util.Logger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.zelretch.aniiiiiict.data.model.WorkImage as WorkImageModel

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val apolloClient: ApolloClient,
    private val logger: Logger
) : AnnictRepository {
    private val TAG = "AnnictRepositoryImpl"
    override suspend fun isAuthenticated(): Boolean {
        val result = tokenManager.hasValidToken()
        logger.info(TAG, "認証状態 = $result", "AnnictRepositoryImpl.isAuthenticated")
        return result
    }

    override suspend fun getAuthUrl(): String {
        return authManager.getAuthorizationUrl()
    }

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
            val response = apolloClient.executeMutation(
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
            authManager.handleAuthorizationCode(code).fold(
                onSuccess = {
                    logger.info(TAG, "認証成功", "AnnictRepositoryImpl.handleAuthCallback")
                    true
                },
                onFailure = { e ->
                    logger.error(
                        TAG,
                        "認証失敗 - ${e.message}",
                        "AnnictRepositoryImpl.handleAuthCallback"
                    )
                    false
                }
            )
        } catch (e: Exception) {
            logger.error(
                TAG,
                "認証処理中に例外が発生 - ${e.message}",
                "AnnictRepositoryImpl.handleAuthCallback"
            )
            false
        }
    }

    override fun getProgramsWithWorks(): Flow<List<ProgramWithWork>> {
        return flow {
            try {
                logger.info(
                    TAG,
                    "プログラム一覧の取得を開始",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                // キャンセルされた場合は例外をスローせずに空のリストを返す
                if (!currentCoroutineContext().isActive) {
                    logger.info(
                        TAG,
                        "処理がキャンセルされたため、実行をスキップします",
                        "AnnictRepositoryImpl.getProgramsWithWorks"
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
                        "AnnictRepositoryImpl.getProgramsWithWorks"
                    )
                    emit(emptyList())
                    return@flow
                }

                val query = ViewerProgramsQuery()
                val response = apolloClient.executeQuery(
                    operation = query,
                    context = "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                logger.info(
                    TAG,
                    "GraphQLのレスポンス: ${response.data != null}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                if (response.hasErrors()) {
                    logger.error(
                        TAG,
                        "GraphQLエラー: ${response.errors}",
                        "AnnictRepositoryImpl.getProgramsWithWorks"
                    )
                    emit(emptyList())
                    return@flow
                }

                val programs = response.data?.viewer?.programs?.nodes
                logger.info(
                    TAG,
                    "取得したプログラム数: ${programs?.size ?: 0}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                val programsWithWorks = processProgramsResponse(programs)
                logger.info(
                    TAG,
                    "変換後のプログラム数: ${programsWithWorks.size}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                emit(programsWithWorks)
            } catch (e: Exception) {
                // キャンセル例外の場合は再スローして上位で処理
                if (e is kotlinx.coroutines.CancellationException) throw e

                logger.error(TAG, e, "プログラム一覧の取得に失敗")
                emit(emptyList())
            }
        }
    }

    private fun processProgramsResponse(responsePrograms: List<ViewerProgramsQuery.Node?>?): List<ProgramWithWork> {
        val programs = responsePrograms?.mapNotNull { node ->
            if (node == null) return@mapNotNull null
            val startedAt = try {
                LocalDateTime.parse(node.startedAt.toString(), DateTimeFormatter.ISO_DATE_TIME)
            } catch (_: Exception) {
                LocalDateTime.now() // パースに失敗した場合は現在時刻を使用
            }

            val channel = Channel(
                name = node.channel.name
            )

            val episode = Episode(
                id = node.episode.id,
                number = node.episode.number,
                numberText = node.episode.numberText,
                title = node.episode.title
            )

            val workImage = node.work.image?.let { image ->
                WorkImageModel(
                    recommendedImageUrl = image.recommendedImageUrl,
                    facebookOgImageUrl = image.facebookOgImageUrl
                )
            }

            val work = Work(
                id = node.work.id,
                title = node.work.title,
                seasonName = node.work.seasonName,
                seasonYear = node.work.seasonYear,
                media = node.work.media.toString(),
                viewerStatusState = node.work.viewerStatusState ?: StatusState.UNKNOWN__,
                image = workImage
            )

            val program = Program(
                id = node.id,
                startedAt = startedAt,
                channel = channel,
                episode = episode
            )

            program to work
        } ?: emptyList()

        // 各作品のプログラムをすべて保持し、最初のエピソードも特定する
        return programs
            .groupBy { it.second.title }
            .map { (_, grouped) ->
                val sortedPrograms = grouped.sortedBy { it.first.episode.number ?: Int.MAX_VALUE }
                val firstProgram = sortedPrograms.firstOrNull()!!
                ProgramWithWork(
                    programs = sortedPrograms.map { it.first },
                    firstProgram = firstProgram.first,
                    work = firstProgram.second
                )
            }
            .sortedBy { it.firstProgram.startedAt }
    }

    override suspend fun getRecords(after: String?): PaginatedRecords = executeApiRequest(
        operation = "getRecords",
        defaultValue = PaginatedRecords(emptyList())
    ) {
        logger.info(TAG, "記録履歴を取得中...", "AnnictRepositoryImpl.getRecords")

        val query =
            ViewerRecordsQuery(after = after?.let { Optional.present(it) } ?: Optional.absent())
        val response = apolloClient.executeQuery(
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
                            seasonNameText = "",
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
        val response = apolloClient.executeMutation(
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
            val response = apolloClient.executeMutation(
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