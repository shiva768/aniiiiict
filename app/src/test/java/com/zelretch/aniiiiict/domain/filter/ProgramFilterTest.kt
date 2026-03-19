package com.zelretch.aniiiiict.domain.filter

import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.Channel
import com.zelretch.aniiiiict.data.model.Episode
import com.zelretch.aniiiiict.data.model.Program
import com.zelretch.aniiiiict.data.model.ProgramWithWork
import com.zelretch.aniiiiict.data.model.Work
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

// テストヘルパー関数はトップレベルに移動
@Suppress("LongParameterList")
fun createProgramWithWork(
    seasonName: SeasonName? = null,
    seasonYear: Int? = null,
    media: String? = null,
    channelName: String? = null,
    status: StatusState? = null,
    title: String = "Test Title",
    startedAt: LocalDateTime? = null
): ProgramWithWork {
    val channel = Channel(name = channelName ?: "Test Channel")
    val episode = Episode(id = "ep1", number = 1, numberText = "1", title = "エピソード1")
    val program = Program(
        id = "prog1",
        startedAt = startedAt ?: LocalDateTime.now(),
        channel = channel,
        episode = episode
    )
    val work = Work(
        id = "work1",
        title = title,
        seasonName = seasonName,
        seasonYear = seasonYear,
        media = media,
        mediaText = media,
        viewerStatusState = status ?: StatusState.NO_STATE
    )
    return ProgramWithWork(listOf(program), work)
}

@DisplayName("ProgramFilter")
class ProgramFilterTest {

    private lateinit var programFilter: ProgramFilter

    @BeforeEach
    fun setup() {
        programFilter = ProgramFilter()
    }

    @Nested
    @DisplayName("利用可能フィルターの抽出")
    inner class ExtractAvailableFilters {

        @Test
        @DisplayName("シーズンの並び順が正しい")
        fun withCorrectOrder() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonName = SeasonName.SUMMER),
                createProgramWithWork(seasonName = SeasonName.WINTER),
                createProgramWithWork(seasonName = SeasonName.AUTUMN),
                createProgramWithWork(seasonName = SeasonName.SPRING)
            )

            // When
            val result = programFilter.extractAvailableFilters(programs)

            // Then
            assertEquals(
                listOf(SeasonName.WINTER, SeasonName.SPRING, SeasonName.SUMMER, SeasonName.AUTUMN),
                result.seasons
            )
        }
    }

    @Nested
    @DisplayName("シーズンフィルター")
    inner class SeasonFilter {

        @Test
        @DisplayName("指定したシーズンのみ返る")
        fun withSelectedSeasons() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonName = SeasonName.WINTER),
                createProgramWithWork(seasonName = SeasonName.SPRING),
                createProgramWithWork(seasonName = SeasonName.SUMMER)
            )
            val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER, SeasonName.SUMMER))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertEquals(setOf(SeasonName.WINTER, SeasonName.SUMMER), result.map { it.work.seasonName }.toSet())
        }

        @Test
        @DisplayName("フィルターが空の場合は全てのプログラムを返す")
        fun withEmptySeasonFilter() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonName = SeasonName.WINTER),
                createProgramWithWork(seasonName = SeasonName.SPRING)
            )
            val filterState = FilterState(selectedSeason = emptySet())

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("シーズンがnullのプログラムはフィルターされる")
        fun withNullSeason() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonName = null),
                createProgramWithWork(seasonName = SeasonName.WINTER)
            )
            val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(SeasonName.WINTER, result[0].work.seasonName)
        }
    }

    @Nested
    @DisplayName("メディアフィルター")
    inner class MediaFilter {

        @Test
        @DisplayName("指定したメディアのみ返る")
        fun withSelectedMedia() {
            // Given
            val programs = listOf(
                createProgramWithWork(media = "TV"),
                createProgramWithWork(media = "MOVIE"),
                createProgramWithWork(media = "OVA")
            )
            val filterState = FilterState(selectedMedia = setOf("TV", "OVA"))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertEquals(setOf("TV", "OVA"), result.map { it.work.media }.toSet())
        }

        @Test
        @DisplayName("フィルターが空の場合は全てのプログラムを返す")
        fun withEmptyMediaFilter() {
            // Given
            val programs = listOf(
                createProgramWithWork(media = "TV"),
                createProgramWithWork(media = "MOVIE")
            )
            val filterState = FilterState(selectedMedia = emptySet())

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("年フィルター")
    inner class YearFilter {

        @Test
        @DisplayName("指定した年のみ返る")
        fun withSelectedYears() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonYear = 2023),
                createProgramWithWork(seasonYear = 2024),
                createProgramWithWork(seasonYear = 2025)
            )
            val filterState = FilterState(selectedYear = setOf(2023, 2025))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertEquals(setOf(2023, 2025), result.map { it.work.seasonYear }.toSet())
        }

        @Test
        @DisplayName("フィルターが空の場合は全てのプログラムを返す")
        fun withEmptyYearFilter() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonYear = 2023),
                createProgramWithWork(seasonYear = 2024)
            )
            val filterState = FilterState(selectedYear = emptySet())

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("境界値が正しく機能する")
        fun withBoundaryValues() {
            // Given
            val programs = listOf(
                createProgramWithWork(seasonYear = 2023),
                createProgramWithWork(seasonYear = 2024),
                createProgramWithWork(seasonYear = 2025)
            )
            val filterState = FilterState(selectedYear = setOf(2024))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(2024, result[0].work.seasonYear)
        }
    }

    @Nested
    @DisplayName("チャンネルフィルター")
    inner class ChannelFilter {

        @Test
        @DisplayName("指定したチャンネルのみ返る")
        fun withSelectedChannels() {
            // Given
            val programs = listOf(
                createProgramWithWork(channelName = "TOKYO MX"),
                createProgramWithWork(channelName = "BS11"),
                createProgramWithWork(channelName = "AT-X")
            )
            val filterState = FilterState(selectedChannel = setOf("TOKYO MX", "AT-X"))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertEquals(setOf("TOKYO MX", "AT-X"), result.map { it.firstProgram.channel.name }.toSet())
        }

        @Test
        @DisplayName("フィルターが空の場合は全てのプログラムを返す")
        fun withEmptyChannelFilter() {
            // Given
            val programs = listOf(
                createProgramWithWork(channelName = "TOKYO MX"),
                createProgramWithWork(channelName = "BS11")
            )
            val filterState = FilterState(selectedChannel = emptySet())

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("ステータスフィルター")
    inner class StatusFilter {

        @Test
        @DisplayName("指定したステータスのみ返る")
        fun withSelectedStatuses() {
            // Given
            val programs = listOf(
                createProgramWithWork(status = StatusState.WATCHING),
                createProgramWithWork(status = StatusState.WANNA_WATCH),
                createProgramWithWork(status = StatusState.NO_STATE)
            )
            val filterState = FilterState(selectedStatus = setOf(StatusState.WATCHING))

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(StatusState.WATCHING, result[0].work.viewerStatusState)
        }

        @Test
        @DisplayName("フィルターが空の場合は全てのプログラムを返す")
        fun withEmptyStatusFilter() {
            // Given
            val programs = listOf(
                createProgramWithWork(status = StatusState.WATCHING),
                createProgramWithWork(status = StatusState.WANNA_WATCH)
            )
            val filterState = FilterState(selectedStatus = emptySet())

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("検索フィルター")
    inner class SearchFilter {

        @Test
        @DisplayName("タイトルで検索できる")
        fun byTitle() {
            // Given
            val programs = listOf(
                createProgramWithWork(title = "テスト作品1"),
                createProgramWithWork(title = "作品2"),
                createProgramWithWork(title = "テスト作品3")
            )
            val filterState = FilterState(searchQuery = "テスト")

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertEquals(setOf("テスト作品1", "テスト作品3"), result.map { it.work.title }.toSet())
        }

        @Test
        @DisplayName("検索クエリが空の場合は全てのプログラムを返す")
        fun withEmptyQuery() {
            // Given
            val programs = listOf(
                createProgramWithWork(title = "テスト作品1"),
                createProgramWithWork(title = "作品2")
            )
            val filterState = FilterState(searchQuery = "")

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("大文字小文字を区別しない")
        fun caseInsensitive() {
            // Given
            val programs = listOf(
                createProgramWithWork(title = "テスト作品A"),
                createProgramWithWork(title = "テスト作品a"),
                createProgramWithWork(title = "作品")
            )
            val filterState = FilterState(searchQuery = "A")

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.work.title.contains("テスト") })
        }

        @Test
        @DisplayName("特殊文字を含む検索クエリが正しく機能する")
        fun withSpecialCharacters() {
            // Given
            val programs = listOf(
                createProgramWithWork(title = "テスト作品!"),
                createProgramWithWork(title = "テスト作品？"),
                createProgramWithWork(title = "テスト作品")
            )
            val filterState = FilterState(searchQuery = "テスト作品!")

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals("テスト作品!", result[0].work.title)
        }
    }

    @Nested
    @DisplayName("放送済みフィルター")
    inner class AiredFilter {

        @Test
        @DisplayName("放送済みのみ返る")
        fun onlyAired() {
            // Given
            val now = LocalDateTime.now()
            val programs = listOf(
                createProgramWithWork(startedAt = now.minusHours(1)), // 放送済み
                createProgramWithWork(startedAt = now.plusHours(1)) // 未放送
            )
            val filterState = FilterState(showOnlyAired = true)

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(now.minusHours(1), result[0].firstProgram.startedAt)
        }

        @Test
        @DisplayName("無効の場合は全てのプログラムを返す")
        fun whenDisabled() {
            // Given
            val now = LocalDateTime.now()
            val programs = listOf(
                createProgramWithWork(startedAt = now.minusHours(1)),
                createProgramWithWork(startedAt = now.plusHours(1))
            )
            val filterState = FilterState(showOnlyAired = false)

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("ソート")
    inner class Sort {

        @Test
        @DisplayName("放送開始時間の昇順ソートが正しく機能する")
        fun sortAscending() {
            // Given
            val now = LocalDateTime.now()
            val programs = listOf(
                createProgramWithWork(startedAt = now.plusHours(2)),
                createProgramWithWork(startedAt = now),
                createProgramWithWork(startedAt = now.plusHours(1))
            )
            val filterState = FilterState(sortOrder = SortOrder.START_TIME_ASC, showOnlyAired = false)

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(3, result.size)
            assertEquals(now, result[0].firstProgram.startedAt)
            assertEquals(now.plusHours(1), result[1].firstProgram.startedAt)
            assertEquals(now.plusHours(2), result[2].firstProgram.startedAt)
        }

        @Test
        @DisplayName("放送開始時間の降順ソートが正しく機能する")
        fun sortDescending() {
            // Given
            val now = LocalDateTime.now()
            val programs = listOf(
                createProgramWithWork(startedAt = now),
                createProgramWithWork(startedAt = now.plusHours(2)),
                createProgramWithWork(startedAt = now.plusHours(1))
            )
            val filterState = FilterState(sortOrder = SortOrder.START_TIME_DESC, showOnlyAired = false)

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(3, result.size)
            assertEquals(now.plusHours(2), result[0].firstProgram.startedAt)
            assertEquals(now.plusHours(1), result[1].firstProgram.startedAt)
            assertEquals(now, result[2].firstProgram.startedAt)
        }
    }

    @Nested
    @DisplayName("複合フィルター")
    inner class CombinedFilters {

        @Test
        @DisplayName("複数のフィルターを組み合わせて正しく機能する")
        fun withMultipleFilters() {
            // Given
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

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(1, result.size)
            assertEquals(SeasonName.WINTER, result[0].work.seasonName)
            assertEquals("TV", result[0].work.media)
            assertEquals("TOKYO MX", result[0].firstProgram.channel.name)
            assertEquals(StatusState.WATCHING, result[0].work.viewerStatusState)
        }

        @Test
        @DisplayName("空のリストを渡すと空のリストを返す")
        fun withEmptyList() {
            // Given
            val programs = emptyList<ProgramWithWork>()
            val filterState = FilterState(
                selectedSeason = setOf(SeasonName.WINTER),
                selectedMedia = setOf("TV")
            )

            // When
            val result = programFilter.applyFilters(programs, filterState)

            // Then
            assertEquals(emptyList<ProgramWithWork>(), result)
        }

        @Test
        @DisplayName("フィルターの順序が結果に影響を与えない")
        fun orderIndependent() {
            // Given
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

            // When
            val result1 = programFilter.applyFilters(programs, filterState1)
            val result2 = programFilter.applyFilters(programs, filterState2)

            // Then
            assertEquals(result1.size, result2.size)
            assertEquals(result1.map { it.work.seasonName }, result2.map { it.work.seasonName })
            assertEquals(result1.map { it.work.media }, result2.map { it.work.media })
        }
    }
}
