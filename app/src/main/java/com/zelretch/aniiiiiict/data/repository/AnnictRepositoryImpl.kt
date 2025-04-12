package com.zelretch.aniiiiiict.data.repository

import com.apollographql.apollo3.api.Optional
import com.zelretch.aniiiiiict.CreateRecordMutation
import com.zelretch.aniiiiiict.DeleteRecordMutation
import com.zelretch.aniiiiiict.UpdateStatusMutation
import com.zelretch.aniiiiiict.ViewerProgramsQuery
import com.zelretch.aniiiiiict.ViewerRecordsQuery
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.dao.WorkImageDao
import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.Channel
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Record
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.data.util.ImageDownloader
import com.zelretch.aniiiiiict.type.StatusState
import com.zelretch.aniiiiiict.util.AniiiiiictLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.zelretch.aniiiiiict.data.model.WorkImage as WorkImageModel
import com.zelretch.aniiiiiict.data.model.PaginatedRecords

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val authManager: AnnictAuthManager,
    private val workImageDao: WorkImageDao,
    private val imageDownloader: ImageDownloader,
    private val apolloClient: ApolloClient
) : AnnictRepository {
    override suspend fun isAuthenticated(): Boolean {
        val result = tokenManager.hasValidToken()
        println("AnnictRepositoryImpl: 認証状態 = $result")
        return result
    }

    override suspend fun getAuthUrl(): String {
        return authManager.getAuthorizationUrl()
    }

    override suspend fun createRecord(episodeId: String, workId: String): Boolean {
        return try {
            // nullや空文字の場合は早期リターン
            if (episodeId.isEmpty() || workId.isEmpty()) {
                AniiiiiictLogger.logError(
                    "エピソードIDまたは作品IDがnullまたは空です",
                    "AnnictRepositoryImpl.createRecord"
                )
                return false
            }

            AniiiiiictLogger.logInfo(
                "エピソード記録を実行: episodeId=$episodeId, workId=$workId",
                "AnnictRepositoryImpl.createRecord"
            )

            // キャンセルされた場合は例外をスローせずに失敗を返す
            if (!currentCoroutineContext().isActive) {
                AniiiiiictLogger.logInfo(
                    "処理がキャンセルされたため、実行をスキップします",
                    "AnnictRepositoryImpl.createRecord"
                )
                return false
            }

            // GraphQL Mutationを実行
            val mutation = CreateRecordMutation(episodeId = episodeId)
            val response = apolloClient.getApolloClient().mutation(mutation).execute()

            AniiiiiictLogger.logInfo(
                "GraphQLのレスポンス: ${response.data != null}, エラー: ${response.errors}",
                "AnnictRepositoryImpl.createRecord"
            )

            !response.hasErrors()
        } catch (e: Exception) {
            AniiiiiictLogger.logError(e, "記録の作成に失敗: episodeId=$episodeId, workId=$workId")
            false
        }
    }

    override suspend fun handleAuthCallback(code: String): Boolean {
        println("AnnictRepositoryImpl: 認証コールバック処理開始 - コード: ${code.take(5)}...")
        return try {
            authManager.handleAuthorizationCode(code).fold(
                onSuccess = {
                    println("AnnictRepositoryImpl: 認証成功")
                    true
                },
                onFailure = { e ->
                    println("AnnictRepositoryImpl: 認証失敗 - ${e.message}")
                    false
                }
            )
        } catch (e: Exception) {
            println("AnnictRepositoryImpl: 認証処理中に例外が発生 - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun saveWorkImage(workId: Long, imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // URLが空か無効な場合は早期リターン
                if (imageUrl.isBlank() || !imageUrl.startsWith("http", ignoreCase = true)) {
                    AniiiiiictLogger.logWarning(
                        "無効な画像URL: '$imageUrl'",
                        "AnnictRepositoryImpl.saveWorkImage"
                    )
                    return@withContext false
                }

                val localPath = imageDownloader.downloadImage(workId, imageUrl)
                // パスが空でなければデータベースに保存
                if (localPath.isNotEmpty()) {
                    workImageDao.insertWorkImage(WorkImage(workId, imageUrl, localPath))
                    true // 成功
                } else {
                    AniiiiiictLogger.logWarning(
                        "画像の保存に失敗 - 空のパス: workId=$workId",
                        "AnnictRepositoryImpl.saveWorkImage"
                    )
                    false // 失敗（空のパス）
                }
            } catch (e: Exception) {
                // エラーがあっても続行できるように例外を処理
                AniiiiiictLogger.logWarning(
                    "画像の保存に失敗: workId=$workId, error=${e.message}",
                    "AnnictRepositoryImpl.saveWorkImage"
                )
                false // 失敗（例外発生）
            }
        }
    }

    override suspend fun getWorkImage(workId: Long): WorkImage? {
        return workImageDao.getWorkImage(workId).firstOrNull()
    }

    override suspend fun getProgramsWithWorks(): Flow<List<ProgramWithWork>> {
        return flow {
            try {
                AniiiiiictLogger.logInfo(
                    "プログラム一覧の取得を開始",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                // キャンセルされた場合は例外をスローせずに空のリストを返す
                if (!currentCoroutineContext().isActive) {
                    AniiiiiictLogger.logInfo(
                        "処理がキャンセルされたため、実行をスキップします",
                        "AnnictRepositoryImpl.getProgramsWithWorks"
                    )
                    emit(emptyList())
                    return@flow
                }

                // アクセストークンの確認
                val token = tokenManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    AniiiiiictLogger.logError(
                        "アクセストークンがありません",
                        "AnnictRepositoryImpl.getProgramsWithWorks"
                    )
                    emit(emptyList())
                    return@flow
                }

                val query = ViewerProgramsQuery()
                val response = apolloClient.getApolloClient().query(query).execute()
                AniiiiiictLogger.logInfo(
                    "GraphQLのレスポンス: ${response.data != null}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                if (response.hasErrors()) {
                    AniiiiiictLogger.logError(
                        "GraphQLエラー: ${response.errors}",
                        "AnnictRepositoryImpl.getProgramsWithWorks"
                    )
                    emit(emptyList())
                    return@flow
                }

                val programs = response.data?.viewer?.programs?.nodes
                AniiiiiictLogger.logInfo(
                    "取得したプログラム数: ${programs?.size ?: 0}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                val programsWithWorks = processProgramsResponse(programs)
                AniiiiiictLogger.logInfo(
                    "変換後のプログラム数: ${programsWithWorks.size}",
                    "AnnictRepositoryImpl.getProgramsWithWorks"
                )

                emit(programsWithWorks)
            } catch (e: Exception) {
                // キャンセル例外の場合は再スローして上位で処理
                if (e is kotlinx.coroutines.CancellationException) throw e

                AniiiiiictLogger.logError(e, "プログラム一覧の取得に失敗")
                emit(emptyList())
            }
        }
    }

    private fun processProgramsResponse(responsePrograms: List<ViewerProgramsQuery.Node?>?): List<ProgramWithWork> {
        val programs = responsePrograms?.mapNotNull { node ->
            if (node == null ) return@mapNotNull null
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
                annictId = node.episode.annictId,
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
                seasonName = node.work.seasonName?.toString(),
                seasonYear = node.work.seasonYear,
                media = node.work.media.toString(),
                viewerStatusState = node.work.viewerStatusState.toString(),
                image = workImage
            )

            val program = Program(
                annictId = try {
                    node.annictId
                } catch (_: Exception) {
                    0 // 変換に失敗した場合は0を使用
                },
                startedAt = startedAt,
                channel = channel,
                episode = episode
            )

            ProgramWithWork(program, work)
        } ?: emptyList()

        // 各作品の最初の未視聴エピソードのみを残す
        return programs
            .groupBy { it.work.title }
            .mapValues { (_, programs) ->
                // エピソード番号でソート（nullや変換できない場合は最後に）
                programs.minByOrNull { program ->
                    program.program.episode.number ?: Int.MAX_VALUE
                }!!
            }
            .values
            .sortedBy { it.program.startedAt }
    }

    override suspend fun getRecords(after: String?): PaginatedRecords {
        if (!currentCoroutineContext().isActive) return PaginatedRecords(emptyList())

        AniiiiiictLogger.logInfo("記録履歴を取得中...", "AnnictRepositoryImpl.getRecords")

        return try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                AniiiiiictLogger.logError(
                    "アクセストークンがありません",
                    "AnnictRepositoryImpl.getRecords"
                )
                return PaginatedRecords(emptyList())
            }

            val query = ViewerRecordsQuery(after = after?.let { Optional.present(it) } ?: Optional.absent())
            val response = apolloClient.getApolloClient().query(query).execute()

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
                                annictId = work.annictId.toLong(),
                                title = work.title,
                                media = null,
                                mediaText = "",
                                viewerStatusState = StatusState.NO_STATE.toString(),
                                seasonNameText = "",
                                imageUrl = work.image?.recommendedImageUrl
                            )
                        )
                    }
                }

                AniiiiiictLogger.logInfo(
                    "${records.size}件の記録を取得しました",
                    "AnnictRepositoryImpl.getRecords"
                )
                PaginatedRecords(
                    records = records,
                    hasNextPage = pageInfo?.hasNextPage == true,
                    endCursor = pageInfo?.endCursor
                )
            } else {
                AniiiiiictLogger.logError(
                    "GraphQLエラー: ${response.errors}",
                    "AnnictRepositoryImpl.getRecords"
                )
                PaginatedRecords(emptyList())
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AniiiiiictLogger.logError(e, "AnnictRepositoryImpl.getRecords")
            PaginatedRecords(emptyList())
        }
    }

    override suspend fun deleteRecord(recordId: String): Boolean {
        if (!currentCoroutineContext().isActive) return false

        AniiiiiictLogger.logInfo("記録を削除中: $recordId", "AnnictRepositoryImpl.deleteRecord")

        return try {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                AniiiiiictLogger.logError(
                    "アクセストークンがありません",
                    "AnnictRepositoryImpl.deleteRecord"
                )
                return false
            }

            val mutation = DeleteRecordMutation(recordId)
            val response = apolloClient.getApolloClient().mutation(mutation).execute()

            if (!response.hasErrors()) {
                AniiiiiictLogger.logInfo(
                    "記録を削除しました: $recordId",
                    "AnnictRepositoryImpl.deleteRecord"
                )
                true
            } else {
                AniiiiiictLogger.logError(
                    "GraphQLエラー: ${response.errors}",
                    "AnnictRepositoryImpl.deleteRecord"
                )
                false
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AniiiiiictLogger.logError(e, "AnnictRepositoryImpl.deleteRecord")
            false
        }
    }

    override suspend fun updateWorkStatus(workId: String, state: StatusState): Boolean {
        return try {
            AniiiiiictLogger.logInfo(
                "作品のステータス更新を実行: workId=$workId, state=$state",
                "AnnictRepositoryImpl.updateWorkStatus"
            )

            if (!currentCoroutineContext().isActive) {
                AniiiiiictLogger.logInfo(
                    "処理がキャンセルされたため、実行をスキップします",
                    "AnnictRepositoryImpl.updateWorkStatus"
                )
                return false
            }

            val mutation = UpdateStatusMutation(
                workId = workId,
                state = state
            )
            val response = apolloClient.getApolloClient().mutation(mutation).execute()

            if (!response.hasErrors()) {
                AniiiiiictLogger.logInfo(
                    "ステータス更新が成功しました: ${response.data?.updateStatus?.work?.id}",
                    "AnnictRepositoryImpl.updateWorkStatus"
                )
                true
            } else {
                AniiiiiictLogger.logError(
                    "GraphQLエラー: ${response.errors}",
                    "AnnictRepositoryImpl.updateWorkStatus"
                )
                false
            }
        } catch (e: Exception) {
            AniiiiiictLogger.logError(e, "作品のステータス更新に失敗: workId=$workId, state=$state")
            false
        }
    }
} 