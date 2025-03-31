package com.zelretch.aniiiiiict.data.repository

import com.apollographql.apollo3.api.ApolloResponse
import com.zelretch.aniiiiiict.GetProgramsQuery
import com.zelretch.aniiiiiict.data.api.AnnictApiClient
import com.zelretch.aniiiiiict.data.api.ApolloClient
import com.zelretch.aniiiiiict.data.auth.AnnictAuthManager
import com.zelretch.aniiiiiict.data.auth.TokenManager
import com.zelretch.aniiiiiict.data.local.dao.WorkImageDao
import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import com.zelretch.aniiiiiict.data.model.AnnictWork
import com.zelretch.aniiiiiict.data.model.Channel
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.data.util.ImageDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import com.zelretch.aniiiiiict.data.model.WorkImage as WorkImageModel

@Singleton
class AnnictRepositoryImpl @Inject constructor(
    private val apiClient: AnnictApiClient,
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

    override suspend fun getWorks(): List<AnnictWork> {
        val watchingWorks = apiClient.getWatchingWorks().getOrThrow()
        val wantToWatchWorks = apiClient.getWantToWatchWorks().getOrThrow()
        return watchingWorks + wantToWatchWorks
    }

    override suspend fun getPrograms(
        unwatched: Boolean
    ): List<ProgramWithWork> {
        // 一時的な実装：GraphQLから取得する
        val result = mutableListOf<ProgramWithWork>()
        try {
            val response = apolloClient.client.query(GetProgramsQuery()).execute()
            result.addAll(processProgramsResponse(response))
        } catch (e: Exception) {
            println("GraphQLクエリの実行中にエラーが発生しました: ${e.message}")
        }
        return result
    }

    override suspend fun createRecord(episodeId: Long) {
        apiClient.createRecord(episodeId).getOrThrow()
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

    override suspend fun saveWorkImage(workId: Long, imageUrl: String) {
        withContext(Dispatchers.IO) {
            val localPath = imageDownloader.downloadImage(workId, imageUrl)
            workImageDao.insertWorkImage(WorkImage(workId, imageUrl, localPath))
        }
    }

    override suspend fun getWorkImage(workId: Long): WorkImage? {
        return workImageDao.getWorkImage(workId).firstOrNull()
    }

    override suspend fun getProgramsWithWorks(): Flow<List<ProgramWithWork>> {
        return flow {
            try {
                println("GraphQLクエリの実行を開始します")
                
                // キャンセルされた場合は例外をスローせずに空のリストを返す
                if (!currentCoroutineContext().isActive) {
                    println("GraphQLクエリがキャンセルされたため、実行をスキップします")
                    emit(emptyList())
                    return@flow
                }
                
                val response = apolloClient.client.query(GetProgramsQuery()).execute()
                println("GraphQLクエリのレスポンス: ${response.data != null}")
                
                if (response.data == null) {
                    println("GraphQLレスポンスにデータがありません。エラー: ${response.errors}")
                    emit(emptyList())
                    return@flow
                }
                
                val programs = response.data?.viewer?.programs?.nodes
                println("取得したプログラム数: ${programs?.size ?: 0}")
                
                val programsWithWorks = processProgramsResponse(response)
                println("変換後のプログラム数: ${programsWithWorks.size}")
                
                emit(programsWithWorks)
            } catch (e: Exception) {
                // キャンセル例外の場合は再スローして上位で処理
                if (e is kotlinx.coroutines.CancellationException) {
                    println("GraphQLクエリの実行中にキャンセルされました")
                    throw e
                }
                
                println("GraphQLクエリの実行中にエラーが発生しました: ${e.message}")
                e.printStackTrace()
                emit(emptyList())
            }
        }
    }

    private fun processProgramsResponse(response: ApolloResponse<GetProgramsQuery.Data>): List<ProgramWithWork> {
        val programs = response.data?.viewer?.programs?.nodes?.mapNotNull { node ->
            if (node == null) return@mapNotNull null

            val startedAt = try {
                LocalDateTime.parse(node.startedAt.toString(), DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                LocalDateTime.now() // パースに失敗した場合は現在時刻を使用
            }

            val channel = Channel(
                name = node.channel.name
            )

            val episode = Episode(
                annictId = node.episode.annictId.toInt(),
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
                title = node.work.title,
                seasonName = node.work.seasonName?.toString(),
                seasonYear = node.work.seasonYear,
                media = node.work.media.toString(),
                viewerStatusState = node.work.viewerStatusState?.toString(),
                image = workImage
            )

            val program = Program(
                annictId = try {
                    node.annictId.toInt()
                } catch (e: Exception) {
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
            .mapValues { (_, programs) -> programs.first() }
            .values
            .sortedByDescending { it.program.startedAt }
    }
} 