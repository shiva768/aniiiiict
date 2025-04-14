package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.data.model.Channel
import com.zelretch.aniiiiiict.data.model.Episode
import com.zelretch.aniiiiiict.data.model.Program
import com.zelretch.aniiiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiiict.data.model.Work
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ProgramFilterTest {
    private val programFilter = ProgramFilter()

    @Test
    fun `シーズンの並び順が正しい`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonName = SeasonName.SUMMER),
            createProgramWithWork(seasonName = SeasonName.WINTER),
            createProgramWithWork(seasonName = SeasonName.AUTUMN),
            createProgramWithWork(seasonName = SeasonName.SPRING)
        )

        // 実行
        val result = programFilter.extractAvailableFilters(programs)

        // 検証
        assertEquals(
            listOf(SeasonName.WINTER, SeasonName.SPRING, SeasonName.SUMMER, SeasonName.AUTUMN),
            result.seasons
        )
    }

    @Test
    fun `シーズンフィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonName = SeasonName.WINTER),
            createProgramWithWork(seasonName = SeasonName.SPRING),
            createProgramWithWork(seasonName = SeasonName.SUMMER)
        )

        val filterState = FilterState(
            selectedSeason = setOf(SeasonName.WINTER, SeasonName.SUMMER)
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.work.seasonName }.containsAll(setOf(SeasonName.WINTER, SeasonName.SUMMER)))
    }

    @Test
    fun `シーズンフィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonName = SeasonName.WINTER),
            createProgramWithWork(seasonName = SeasonName.SPRING)
        )

        val filterState = FilterState(selectedSeason = emptySet())

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `シーズンがnullのプログラムはフィルターされない`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonName = null),
            createProgramWithWork(seasonName = SeasonName.WINTER)
        )

        val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals(SeasonName.WINTER, result[0].work.seasonName)
    }

    @Test
    fun `メディアフィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(media = "TV"),
            createProgramWithWork(media = "MOVIE"),
            createProgramWithWork(media = "OVA")
        )

        val filterState = FilterState(
            selectedMedia = setOf("TV", "OVA")
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.work.media }.containsAll(setOf("TV", "OVA")))
    }

    @Test
    fun `メディアフィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(media = "TV"),
            createProgramWithWork(media = "MOVIE")
        )

        val filterState = FilterState(selectedMedia = emptySet())

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `年フィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonYear = 2023),
            createProgramWithWork(seasonYear = 2024),
            createProgramWithWork(seasonYear = 2025)
        )

        val filterState = FilterState(
            selectedYear = setOf(2023, 2025)
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.work.seasonYear }.containsAll(setOf(2023, 2025)))
    }

    @Test
    fun `年フィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonYear = 2023),
            createProgramWithWork(seasonYear = 2024)
        )

        val filterState = FilterState(selectedYear = emptySet())

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `年フィルターの境界値テスト`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(seasonYear = 2023),
            createProgramWithWork(seasonYear = 2024),
            createProgramWithWork(seasonYear = 2025)
        )

        val filterState = FilterState(
            selectedYear = setOf(2024)
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals(2024, result[0].work.seasonYear)
    }

    @Test
    fun `チャンネルフィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(channelName = "TOKYO MX"),
            createProgramWithWork(channelName = "BS11"),
            createProgramWithWork(channelName = "AT-X")
        )

        val filterState = FilterState(
            selectedChannel = setOf("TOKYO MX", "AT-X")
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.firstProgram.channel.name }
            .containsAll(setOf("TOKYO MX", "AT-X")))
    }

    @Test
    fun `チャンネルフィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(channelName = "TOKYO MX"),
            createProgramWithWork(channelName = "BS11")
        )

        val filterState = FilterState(selectedChannel = emptySet())

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `ステータスフィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(status = StatusState.WATCHING),
            createProgramWithWork(status = StatusState.WANNA_WATCH),
            createProgramWithWork(status = StatusState.NO_STATE)
        )

        val filterState = FilterState(
            selectedStatus = setOf(StatusState.WATCHING)
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals(StatusState.WATCHING, result[0].work.viewerStatusState)
    }

    @Test
    fun `ステータスフィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(status = StatusState.WATCHING),
            createProgramWithWork(status = StatusState.WANNA_WATCH)
        )

        val filterState = FilterState(selectedStatus = emptySet())

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `検索フィルターが正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(title = "テスト作品1"),
            createProgramWithWork(title = "作品2"),
            createProgramWithWork(title = "テスト作品3")
        )

        val filterState = FilterState(searchQuery = "テスト")

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.map { it.work.title }.containsAll(setOf("テスト作品1", "テスト作品3")))
    }

    @Test
    fun `検索フィルターが空の場合は全てのプログラムを返す`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(title = "テスト作品1"),
            createProgramWithWork(title = "作品2")
        )

        val filterState = FilterState(searchQuery = "")

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `検索フィルターは大文字小文字を区別しない`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(title = "テスト作品A"),
            createProgramWithWork(title = "テスト作品a"),
            createProgramWithWork(title = "作品")
        )

        val filterState = FilterState(searchQuery = "A")

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
        assertTrue(result.all { it.work.title.contains("テスト") })
    }

    @Test
    fun `放送済みフィルターが正しく機能する`() {
        // 準備
        val now = LocalDateTime.now()
        val programs = listOf(
            createProgramWithWork(startedAt = now.minusHours(1)), // 放送済み
            createProgramWithWork(startedAt = now.plusHours(1))   // 未放送
        )

        val filterState = FilterState(showOnlyAired = true)

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals(now.minusHours(1), result[0].firstProgram.startedAt)
    }

    @Test
    fun `放送済みフィルターが無効の場合は全てのプログラムを返す`() {
        // 準備
        val now = LocalDateTime.now()
        val programs = listOf(
            createProgramWithWork(startedAt = now.minusHours(1)),
            createProgramWithWork(startedAt = now.plusHours(1))
        )

        val filterState = FilterState(showOnlyAired = false)

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(2, result.size)
    }

    @Test
    fun `放送開始時間の昇順ソートが正しく機能する`() {
        // 準備
        val now = LocalDateTime.now()
        val programs = listOf(
            createProgramWithWork(startedAt = now.plusHours(2)),
            createProgramWithWork(startedAt = now),
            createProgramWithWork(startedAt = now.plusHours(1))
        )

        val filterState = FilterState(sortOrder = SortOrder.START_TIME_ASC, showOnlyAired = false)

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(3, result.size)
        assertEquals(now, result[0].firstProgram.startedAt)
        assertEquals(now.plusHours(1), result[1].firstProgram.startedAt)
        assertEquals(now.plusHours(2), result[2].firstProgram.startedAt)
    }

    @Test
    fun `放送開始時間の降順ソートが正しく機能する`() {
        // 準備
        val now = LocalDateTime.now()
        val programs = listOf(
            createProgramWithWork(startedAt = now),
            createProgramWithWork(startedAt = now.plusHours(2)),
            createProgramWithWork(startedAt = now.plusHours(1))
        )

        val filterState = FilterState(sortOrder = SortOrder.START_TIME_DESC, showOnlyAired = false)

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(3, result.size)
        assertEquals(now.plusHours(2), result[0].firstProgram.startedAt)
        assertEquals(now.plusHours(1), result[1].firstProgram.startedAt)
        assertEquals(now, result[2].firstProgram.startedAt)
    }

    @Test
    fun `複数のフィルターを組み合わせた場合に正しく機能する`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(
                seasonName = SeasonName.WINTER,
                media = "TV",
                channelName = "TOKYO MX",
                status = StatusState.WATCHING
            ),
            createProgramWithWork(
                seasonName = SeasonName.WINTER,
                media = "TV",
                channelName = "BS11",
                status = StatusState.WANNA_WATCH
            ),
            createProgramWithWork(
                seasonName = SeasonName.SUMMER,
                media = "OVA",
                channelName = "AT-X",
                status = StatusState.WATCHING
            )
        )

        val filterState = FilterState(
            selectedSeason = setOf(SeasonName.WINTER),
            selectedMedia = setOf("TV"),
            selectedChannel = setOf("TOKYO MX"),
            selectedStatus = setOf(StatusState.WATCHING)
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals(SeasonName.WINTER, result[0].work.seasonName)
        assertEquals("TV", result[0].work.media)
        assertEquals("TOKYO MX", result[0].firstProgram.channel.name)
        assertEquals(StatusState.WATCHING, result[0].work.viewerStatusState)
    }

    @Test
    fun `空のリストに対してフィルタリングを行った場合`() {
        // 準備
        val programs = emptyList<ProgramWithWork>()
        val filterState = FilterState(
            selectedSeason = setOf(SeasonName.WINTER),
            selectedMedia = setOf("TV")
        )

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertTrue(result.isEmpty())
    }

    @Test
    fun `フィルターの順序が結果に影響を与えない`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(
                seasonName = SeasonName.WINTER,
                media = "TV",
                channelName = "TOKYO MX"
            ),
            createProgramWithWork(
                seasonName = SeasonName.SUMMER,
                media = "OVA",
                channelName = "AT-X"
            )
        )

        val filterState1 = FilterState(
            selectedSeason = setOf(SeasonName.WINTER),
            selectedMedia = setOf("TV")
        )

        val filterState2 = FilterState(
            selectedMedia = setOf("TV"),
            selectedSeason = setOf(SeasonName.WINTER)
        )

        // 実行
        val result1 = programFilter.applyFilters(programs, filterState1)
        val result2 = programFilter.applyFilters(programs, filterState2)

        // 検証
        assertEquals(result1.size, result2.size)
        assertEquals(result1.map { it.work.seasonName }, result2.map { it.work.seasonName })
        assertEquals(result1.map { it.work.media }, result2.map { it.work.media })
    }

    @Test
    fun `特殊文字を含む検索クエリのテスト`() {
        // 準備
        val programs = listOf(
            createProgramWithWork(title = "テスト作品!"),
            createProgramWithWork(title = "テスト作品？"),
            createProgramWithWork(title = "テスト作品")
        )

        val filterState = FilterState(searchQuery = "テスト作品!")

        // 実行
        val result = programFilter.applyFilters(programs, filterState)

        // 検証
        assertEquals(1, result.size)
        assertEquals("テスト作品!", result[0].work.title)
    }

    private fun createProgramWithWork(
        seasonName: SeasonName? = null,
        seasonYear: Int? = null,
        media: String? = null,
        status: StatusState = StatusState.WATCHING,
        title: String = "テスト作品",
        channelName: String = "テストチャンネル",
        startedAt: LocalDateTime = LocalDateTime.now()
    ): ProgramWithWork {
        val work = Work(
            id = "1",
            title = title,
            seasonName = seasonName,
            seasonYear = seasonYear,
            media = media,
            mediaText = media,
            viewerStatusState = status
        )

        val program = Program(
            id = "1",
            startedAt = startedAt,
            channel = Channel(name = channelName),
            episode = Episode(
                id = "1",
                number = 1,
                numberText = "1",
                title = "テストエピソード"
            )
        )

        return ProgramWithWork(listOf(program), program, work)
    }
} 