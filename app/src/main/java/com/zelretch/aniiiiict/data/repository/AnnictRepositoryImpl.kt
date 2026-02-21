package com.zelretch.aniiiiict.data.repository

import com.annict.CreateRecordMutation
import com.annict.DeleteRecordMutation
import com.annict.UpdateStatusMutation
import com.annict.ViewerProgramsQuery
import com.annict.ViewerRecordsQuery
import com.annict.WorkDetailQuery
import com.annict.type.StatusState
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import com.zelretch.aniiiiict.data.api.AnnictApolloClient
import com.zelretch.aniiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiict.data.auth.TokenManager
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.LibraryEntry
import com.zelretch.aniiiiict.data.model.PaginatedRecords
import com.zelretch.aniiiiict.data.model.Record
import com.zelretch.aniiiiict.data.model.Work
import com.zelretch.aniiiiict.data.model.WorkImage
import com.zelretch.aniiiiict.domain.error.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
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

    override suspend fun createRecord(episodeId: String, workId: String): Result<Unit> {
        if (episodeId.isEmpty() || workId.isEmpty()) {
            Timber.e("エピソードIDまたは作品IDがnullまたは空です")
            return Result.failure(
                DomainError.ValidationError.MissingRequiredParameter("episodeId or workId")
            )
        }

        return executeApiRequest("createRecord") {
            Timber.i("エピソード記録を実行: episodeId=$episodeId, workId=$workId")

            val mutation = CreateRecordMutation(episodeId = episodeId)
            val response = annictApolloClient.executeMutation(
                operation = mutation,
                context = "AnnictRepositoryImpl.createRecord"
            )

            Timber.i("GraphQLのレスポンス: ${response.data != null}, エラー: ${response.errors}")

            if (response.hasErrors()) {
                throw DomainError.BusinessError.RecordCreationFailed()
            }
        }
    }

    override suspend fun handleAuthCallback(code: String): Result<Unit> {
        Timber.i("認証コールバック処理開始 - コード: ${code.take(AUTH_CODE_LOG_LENGTH)}...")
        return executeApiRequest("handleAuthCallback") {
            authManager.handleAuthorizationCode(code).getOrElse { e ->
                throw DomainError.AuthError.CallbackFailed(e)
            }
            Timber.i("認証成功")
        }
    }

    override suspend fun getRawProgramsData(): Result<List<ViewerProgramsQuery.Node?>> =
        executeApiRequest("getRawProgramsData") {
            Timber.i("プログラム一覧の取得を開始")

            val query = ViewerProgramsQuery()
            val response = annictApolloClient.executeQuery(
                operation = query,
                context = "AnnictRepositoryImpl.getRawProgramsData"
            )

            Timber.i("GraphQLのレスポンス: ${response.data != null}")

            if (response.hasErrors()) {
                Timber.e("GraphQLエラー: ${response.errors}")
                throw DomainError.ApiError.GraphQLError("Programs query failed: ${response.errors}")
            }

            val programs = response.data?.viewer?.programs?.nodes
            Timber.i("取得したプログラム数: ${programs?.size ?: 0}")
            programs ?: emptyList()
        }

    override suspend fun getRecords(after: String?): Result<PaginatedRecords> = executeApiRequest("getRecords") {
        Timber.i("記録履歴を取得中...")

        val query = ViewerRecordsQuery(
            after = after?.let { Optional.present(it) } ?: Optional.absent()
        )
        val response = annictApolloClient.executeQuery(
            operation = query,
            context = "AnnictRepositoryImpl.getRecords"
        )

        if (response.hasErrors()) {
            Timber.e("GraphQLエラー: ${response.errors}")
            throw DomainError.BusinessError.RecordsLoadFailed()
        }

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

        Timber.i("${records.size}件の記録を取得しました")
        PaginatedRecords(
            records = records,
            hasNextPage = pageInfo?.hasNextPage == true,
            endCursor = pageInfo?.endCursor
        )
    }

    override suspend fun deleteRecord(recordId: String): Result<Unit> = executeApiRequest("deleteRecord") {
        Timber.i("記録を削除中: $recordId")

        val mutation = DeleteRecordMutation(recordId)
        val response = annictApolloClient.executeMutation(
            operation = mutation,
            context = "AnnictRepositoryImpl.deleteRecord"
        )

        if (response.hasErrors()) {
            Timber.e("GraphQLエラー: ${response.errors}")
            throw DomainError.BusinessError.RecordDeletionFailed()
        }

        Timber.i("記録を削除しました: $recordId")
    }

    override suspend fun updateWorkViewStatus(workId: String, state: StatusState): Result<Unit> =
        executeApiRequest("updateWorkStatus") {
            Timber.i("作品ステータスを更新中: workId=$workId, state=$state")

            val mutation = UpdateStatusMutation(workId = workId, state = state)
            val response = annictApolloClient.executeMutation(
                operation = mutation,
                context = "AnnictRepositoryImpl.updateWorkStatus"
            )

            if (response.hasErrors()) {
                Timber.e("GraphQLエラー: ${response.errors}")
                throw DomainError.BusinessError.StatusUpdateFailed()
            }

            Timber.i("作品ステータスを更新しました: workId=$workId, state=$state")
        }

    override suspend fun getWorkDetail(workId: String): Result<WorkDetailQuery.Node?> =
        executeApiRequest("getWorkDetail") {
            Timber.i("作品詳細情報を取得中: workId=$workId")

            val query = WorkDetailQuery(workId = workId)
            val response = annictApolloClient.executeQuery(
                operation = query,
                context = "AnnictRepositoryImpl.getWorkDetail"
            )

            if (response.hasErrors()) {
                Timber.e("GraphQLエラー: ${response.errors}")
                throw DomainError.ApiError.GraphQLError("Work detail query failed: ${response.errors}")
            }

            val node = response.data?.node
            Timber.i("作品詳細情報を取得しました: workId=$workId")
            node
        }

    override suspend fun getLibraryEntries(states: List<StatusState>, after: String?): Result<List<LibraryEntry>> =
        executeApiRequest("getLibraryEntries") {
            Timber.i("ライブラリエントリー一覧の取得を開始: states=$states")

            val query = com.annict.ViewerLibraryEntriesQuery(
                states = Optional.present(states),
                after = Optional.presentIfNotNull(after)
            )
            val response = annictApolloClient.executeQuery(
                operation = query,
                context = "AnnictRepositoryImpl.getLibraryEntries"
            )

            Timber.i("GraphQLのレスポンス: ${response.data != null}")

            if (response.hasErrors()) {
                Timber.e("GraphQLエラー: ${response.errors}")
                throw DomainError.ApiError.GraphQLError("Library entries query failed: ${response.errors}")
            }

            val nodes = response.data?.viewer?.libraryEntries?.nodes?.filterNotNull() ?: emptyList()
            val entries = nodes.mapNotNull { node -> mapToLibraryEntry(node) }

            Timber.i("ライブラリエントリー一覧を取得しました: ${entries.size}件")
            entries
        }

    private fun mapToLibraryEntry(node: com.annict.ViewerLibraryEntriesQuery.Node): LibraryEntry? {
        val viewerStatus = node.work.viewerStatusState ?: return null
        return LibraryEntry(
            id = node.id,
            work = Work(
                id = node.work.id,
                title = node.work.title,
                seasonName = node.work.seasonName,
                seasonYear = node.work.seasonYear,
                media = node.work.media.rawValue,
                malAnimeId = node.work.malAnimeId,
                viewerStatusState = viewerStatus,
                noEpisodes = node.work.noEpisodes,
                image = node.work.image?.let { image ->
                    WorkImage(
                        recommendedImageUrl = image.recommendedImageUrl,
                        facebookOgImageUrl = image.facebookOgImageUrl
                    )
                }
            ),
            nextEpisode = node.nextEpisode?.let { episode ->
                Episode(
                    id = episode.id,
                    number = episode.number,
                    numberText = episode.numberText,
                    title = episode.title
                )
            },
            statusState = node.status?.state
        )
    }

    /**
     * 共通のAPIリクエスト処理を行うヘルパーメソッド
     * エラーをDomainErrorにマッピングしてResult<T>で返す
     */
    private suspend fun <T> executeApiRequest(operation: String, request: suspend () -> T): Result<T> {
        val token = tokenManager.getAccessToken()
        if (!currentCoroutineContext().isActive) {
            return Result.failure(DomainError.Unknown("処理がキャンセルされました"))
        }
        if (token.isNullOrEmpty()) {
            Timber.w("トークンが未設定: operation=$operation")
            return Result.failure(DomainError.AuthError.InvalidToken())
        }

        return try {
            Result.success(request())
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainError) {
            Timber.e(e, "[$operation] Domain error")
            Result.failure(e)
        } catch (e: ApolloException) {
            Timber.e(e, "[$operation] API error")
            Result.failure(DomainError.ApiError.Unknown(e))
        } catch (e: IOException) {
            Timber.e(e, "[$operation] Network IO error")
            Result.failure(DomainError.NetworkError.NoConnection(e))
        } catch (e: Exception) {
            Timber.e(e, "[$operation] Unexpected error")
            Result.failure(DomainError.Unknown(cause = e))
        }
    }
}
