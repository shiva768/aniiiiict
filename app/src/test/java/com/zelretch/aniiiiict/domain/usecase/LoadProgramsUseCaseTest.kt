package com.zelretch.aniiiiict.domain.usecase

import com.annict.ViewerProgramsQuery
import com.annict.type.Media
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.repository.AnnictRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LoadProgramsUseCase")
class LoadProgramsUseCaseTest {

    private lateinit var repository: AnnictRepository
    private lateinit var useCase: LoadProgramsUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = LoadProgramsUseCase(repository)
    }

    @Nested
    @DisplayName("プログラムの読み込み")
    inner class LoadPrograms {

        @Test
        @DisplayName("複数の作品と複数のエピソードがある場合作品ごとにグループ化されエピソード番号でソートされる")
        fun groupedAndSorted() = runTest {
            // Given
            val mockPrograms = TestHelper.createMockPrograms()
            coEvery { repository.getRawProgramsData() } returns flow { emit(mockPrograms) }

            // When
            val result = useCase().first()

            // Then
            assertEquals(2, result.size) // 2つの異なる作品

            // 作品1の検証
            val anime1 = result.find { it.work.title == "アニメ1" }
            assertNotNull(anime1)
            assertEquals(2, anime1!!.programs.size) // アニメ1は2つのエピソード
            assertEquals("#1", anime1.firstProgram.episode.numberText) // 最初のエピソードが設定されている

            // 作品2の検証
            val anime2 = result.find { it.work.title == "アニメ2" }
            assertNotNull(anime2)
            assertEquals(1, anime2!!.programs.size) // アニメ2は1つのエピソード
            assertEquals("#1", anime2.firstProgram.episode.numberText) // 最初のエピソード

            // 日付順にソートされているか検証
            assertTrue(
                result[0].firstProgram.startedAt.isBefore(result[1].firstProgram.startedAt)
            )
        }

        @Test
        @DisplayName("同じ作品に複数のエピソードがある場合エピソード番号でソートされる")
        fun sortedByEpisodeNumber() = runTest {
            // Given
            val nodes = TestHelper.createMockEpisodesForSameAnime()
            coEvery { repository.getRawProgramsData() } returns flow { emit(nodes) }

            // When
            val result = useCase().first()

            // Then
            assertEquals(1, result.size) // 1つの作品
            val anime = result[0]
            assertEquals(3, anime.programs.size) // 3つのエピソード

            // エピソードが番号順にソートされているか検証
            assertEquals(1, anime.programs[0].episode.number)
            assertEquals(2, anime.programs[1].episode.number)
            assertEquals(3, anime.programs[2].episode.number)

            // firstProgramが最初のエピソードになっているか
            assertEquals(1, anime.firstProgram.episode.number)
        }

        @Test
        @DisplayName("空のデータが返された場合空のリストが返される")
        fun withEmptyData() = runTest {
            // Given
            coEvery { repository.getRawProgramsData() } returns flow { emit(emptyList()) }

            // When
            val result = useCase().first()

            // Then
            assertEquals(0, result.size)
        }
    }
}

// TestHelperオブジェクトをトップレベルに定義
object TestHelper {
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
        workTitle: String = "作品タイトル",
        hasNextEpisode: Boolean = true
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

        // nextEpisodeのモック設定
        if (hasNextEpisode) {
            val nextEpisode = mockk<ViewerProgramsQuery.NextEpisode>()
            every { nextEpisode.number } returns (episodeNumber ?: 0) + 1
            every { episode.nextEpisode } returns nextEpisode
        } else {
            every { episode.nextEpisode } returns null
        }

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
