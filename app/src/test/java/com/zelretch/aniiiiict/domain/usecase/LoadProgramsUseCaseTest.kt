package com.zelretch.aniiiiict.domain.usecase

import com.annict.ViewerProgramsQuery
import com.annict.type.Media
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class プログラム読み込みユースケーステスト : BehaviorSpec({
    // コンパニオンオブジェクトからヘルパーメソッドを参照
    val helper = TestHelper
    val repository = mockk<AnnictRepository>()
    val useCase = LoadProgramsUseCase(repository)

    前提("プログラムがリポジトリに存在する場合") {
        場合("複数の作品と複数のエピソードがある場合") {
            そのとき("作品ごとにグループ化されて、エピソード番号でソートされた結果が返される") {
                // テスト用のモックデータを作成
                val mockPrograms = helper.createMockPrograms()
                coEvery { repository.getRawProgramsData() } returns flow { emit(mockPrograms) }

                // UseCaseを実行
                val result = useCase().first()

                // 結果の検証
                result shouldHaveSize 2 // 2つの異なる作品

                // 作品1の検証
                val anime1 = result.find { it.work.title == "アニメ1" }
                anime1 shouldNotBe null
                anime1!!.programs shouldHaveSize 2 // アニメ1は2つのエピソード
                anime1.firstProgram.episode.numberText shouldBe "#1" // 最初のエピソードが設定されている

                // 作品2の検証
                val anime2 = result.find { it.work.title == "アニメ2" }
                anime2 shouldNotBe null
                anime2!!.programs shouldHaveSize 1 // アニメ2は1つのエピソード
                anime2.firstProgram.episode.numberText shouldBe "#1" // 最初のエピソード

                // 日付順にソートされているか検証
                result[0].firstProgram.startedAt.isBefore(
                    result[1].firstProgram.startedAt
                ) shouldBe true
            }
        }

        場合("同じ作品に複数のエピソードがある場合") {
            そのとき("エピソード番号でソートされる") {
                // 同じ作品で番号の異なる複数エピソードを持つデータ
                val nodes = helper.createMockEpisodesForSameAnime()
                coEvery { repository.getRawProgramsData() } returns flow { emit(nodes) }

                val result = useCase().first()

                result shouldHaveSize 1 // 1つの作品
                val anime = result[0]
                anime.programs shouldHaveSize 3 // 3つのエピソード

                // エピソードが番号順にソートされているか検証
                anime.programs[0].episode.number shouldBe 1
                anime.programs[1].episode.number shouldBe 2
                anime.programs[2].episode.number shouldBe 3

                // firstProgramが最初のエピソードになっているか
                anime.firstProgram.episode.number shouldBe 1
            }
        }

        場合("空のデータが返された場合") {
            そのとき("空のリストが返される") {
                coEvery { repository.getRawProgramsData() } returns flow { emit(emptyList()) }

                val result = useCase().first()
                result shouldHaveSize 0
            }
        }
    }
}) {
    // TestHelperオブジェクトを内部クラスとして定義
    object TestHelper {
        // テスト用のモックデータを生成するヘルパーメソッド
        fun createMockPrograms(): List<ViewerProgramsQuery.Node> {
            // アニメ1: 2つのエピソード
            val anime1Episode1 = createMockNode(
                id = "prog1",
                startedAt = "2025-01-01T12:00:00Z",
                channelName = "テレビ東京",
                episodeId = "ep1",
                episodeNumber = 1,
                episodeNumberText = "#1",
                episodeTitle = "はじまり",
                workId = "work1",
                workTitle = "アニメ1"
            )

            val anime1Episode2 = createMockNode(
                id = "prog2",
                startedAt = "2025-01-08T12:00:00Z",
                channelName = "テレビ東京",
                episodeId = "ep2",
                episodeNumber = 2,
                episodeNumberText = "#2",
                episodeTitle = "続き",
                workId = "work1",
                workTitle = "アニメ1"
            )

            // アニメ2: 1つのエピソード
            val anime2Episode1 = createMockNode(
                id = "prog3",
                startedAt = "2025-01-02T15:00:00Z",
                channelName = "TOKYO MX",
                episodeId = "ep3",
                episodeNumber = 1,
                episodeNumberText = "#1",
                episodeTitle = "スタート",
                workId = "work2",
                workTitle = "アニメ2"
            )

            return listOf(anime1Episode1, anime1Episode2, anime2Episode1)
        }

        fun createMockEpisodesForSameAnime(): List<ViewerProgramsQuery.Node> {
            // エピソード順をわざと入れ替えて、ソートが正しく機能するか確認
            val episode2 = createMockNode(
                id = "prog2",
                startedAt = "2025-01-15T12:00:00Z",
                episodeNumber = 2,
                episodeNumberText = "#2",
                workTitle = "テストアニメ"
            )

            val episode1 = createMockNode(
                id = "prog1",
                startedAt = "2025-01-08T12:00:00Z",
                episodeNumber = 1,
                episodeNumberText = "#1",
                workTitle = "テストアニメ"
            )

            val episode3 = createMockNode(
                id = "prog3",
                startedAt = "2025-01-22T12:00:00Z",
                episodeNumber = 3,
                episodeNumberText = "#3",
                workTitle = "テストアニメ"
            )

            return listOf(episode2, episode1, episode3)
        }

        @Suppress("LongParameterList")
        fun createMockNode(
            id: String = "prog-id",
            startedAt: String = "2025-01-01T12:00:00Z",
            channelName: String = "テレビ東京",
            episodeId: String = "ep-id",
            episodeNumber: Int? = 1,
            episodeNumberText: String = "#1",
            episodeTitle: String = "エピソードタイトル",
            workId: String = "work-id",
            workTitle: String = "作品タイトル"
        ): ViewerProgramsQuery.Node {
            val node = mockk<ViewerProgramsQuery.Node>()

            every { node.id } returns id
            every { node.startedAt } returns startedAt

            val channel = mockk<ViewerProgramsQuery.Channel>()
            every { channel.name } returns channelName
            every { node.channel } returns channel

            val episode = mockk<ViewerProgramsQuery.Episode>()
            every { episode.id } returns episodeId
            every { episode.number } returns episodeNumber
            every { episode.numberText } returns episodeNumberText
            every { episode.title } returns episodeTitle
            every { node.episode } returns episode

            val work = mockk<ViewerProgramsQuery.Work>()
            every { work.id } returns workId
            every { work.title } returns workTitle
            every { work.seasonName } returns SeasonName.WINTER
            every { work.seasonYear } returns 2025
            every { work.media } returns Media.TV
            every { work.malAnimeId } returns "456"
            every { work.viewerStatusState } returns StatusState.WATCHING
            every { work.image } returns null
            every { node.work } returns work

            return node
        }
    }
}
