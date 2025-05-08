package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.data.model.*
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

// テストヘルパー関数はトップレベルに移動
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
    return ProgramWithWork(listOf(program), program, work)
}

class ProgramFilterTest : BehaviorSpec({
    val programFilter = ProgramFilter()

    given("複数のProgramWithWorkがあるとき") {
        `when`("extractAvailableFiltersを呼び出した場合") {
            then("シーズンの並び順が正しい") {
                val programs = listOf(
                    createProgramWithWork(seasonName = SeasonName.SUMMER),
                    createProgramWithWork(seasonName = SeasonName.WINTER),
                    createProgramWithWork(seasonName = SeasonName.AUTUMN),
                    createProgramWithWork(seasonName = SeasonName.SPRING)
                )
                val result = programFilter.extractAvailableFilters(programs)
                result.seasons shouldBe listOf(
                    SeasonName.WINTER,
                    SeasonName.SPRING,
                    SeasonName.SUMMER,
                    SeasonName.AUTUMN
                )
            }
        }
        `when`("applyFiltersでシーズンフィルターを適用した場合") {
            then("シーズンフィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(seasonName = SeasonName.WINTER),
                    createProgramWithWork(seasonName = SeasonName.SPRING),
                    createProgramWithWork(seasonName = SeasonName.SUMMER)
                )
                val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER, SeasonName.SUMMER))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.map { it.work.seasonName }.toSet() shouldBe setOf(SeasonName.WINTER, SeasonName.SUMMER)
            }
            then("シーズンフィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(seasonName = SeasonName.WINTER),
                    createProgramWithWork(seasonName = SeasonName.SPRING)
                )
                val filterState = FilterState(selectedSeason = emptySet())
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersでシーズンがnullのプログラムがある場合") {
            then("シーズンがnullのプログラムはフィルターされない") {
                val programs = listOf(
                    createProgramWithWork(seasonName = null),
                    createProgramWithWork(seasonName = SeasonName.WINTER)
                )
                val filterState = FilterState(selectedSeason = setOf(SeasonName.WINTER))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].work.seasonName shouldBe SeasonName.WINTER
            }
        }
        `when`("applyFiltersでメディアフィルターを適用した場合") {
            then("メディアフィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(media = "TV"),
                    createProgramWithWork(media = "MOVIE"),
                    createProgramWithWork(media = "OVA")
                )
                val filterState = FilterState(selectedMedia = setOf("TV", "OVA"))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.map { it.work.media }.toSet() shouldBe setOf("TV", "OVA")
            }
            then("メディアフィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(media = "TV"),
                    createProgramWithWork(media = "MOVIE")
                )
                val filterState = FilterState(selectedMedia = emptySet())
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersで年フィルターを適用した場合") {
            then("年フィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(seasonYear = 2023),
                    createProgramWithWork(seasonYear = 2024),
                    createProgramWithWork(seasonYear = 2025)
                )
                val filterState = FilterState(selectedYear = setOf(2023, 2025))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.map { it.work.seasonYear }.toSet() shouldBe setOf(2023, 2025)
            }
            then("年フィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(seasonYear = 2023),
                    createProgramWithWork(seasonYear = 2024)
                )
                val filterState = FilterState(selectedYear = emptySet())
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersで年フィルターの境界値をテストする場合") {
            then("年フィルターの境界値が正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(seasonYear = 2023),
                    createProgramWithWork(seasonYear = 2024),
                    createProgramWithWork(seasonYear = 2025)
                )
                val filterState = FilterState(selectedYear = setOf(2024))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].work.seasonYear shouldBe 2024
            }
        }
        `when`("applyFiltersでチャンネルフィルターを適用した場合") {
            then("チャンネルフィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(channelName = "TOKYO MX"),
                    createProgramWithWork(channelName = "BS11"),
                    createProgramWithWork(channelName = "AT-X")
                )
                val filterState = FilterState(selectedChannel = setOf("TOKYO MX", "AT-X"))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.map { it.firstProgram.channel.name }.toSet() shouldBe setOf("TOKYO MX", "AT-X")
            }
            then("チャンネルフィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(channelName = "TOKYO MX"),
                    createProgramWithWork(channelName = "BS11")
                )
                val filterState = FilterState(selectedChannel = emptySet())
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersでステータスフィルターを適用した場合") {
            then("ステータスフィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(status = StatusState.WATCHING),
                    createProgramWithWork(status = StatusState.WANNA_WATCH),
                    createProgramWithWork(status = StatusState.NO_STATE)
                )
                val filterState = FilterState(selectedStatus = setOf(StatusState.WATCHING))
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].work.viewerStatusState shouldBe StatusState.WATCHING
            }
            then("ステータスフィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(status = StatusState.WATCHING),
                    createProgramWithWork(status = StatusState.WANNA_WATCH)
                )
                val filterState = FilterState(selectedStatus = emptySet())
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersで検索フィルターを適用した場合") {
            then("検索フィルターが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(title = "テスト作品1"),
                    createProgramWithWork(title = "作品2"),
                    createProgramWithWork(title = "テスト作品3")
                )
                val filterState = FilterState(searchQuery = "テスト")
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.map { it.work.title }.toSet() shouldBe setOf("テスト作品1", "テスト作品3")
            }
            then("検索フィルターが空の場合は全てのプログラムを返す") {
                val programs = listOf(
                    createProgramWithWork(title = "テスト作品1"),
                    createProgramWithWork(title = "作品2")
                )
                val filterState = FilterState(searchQuery = "")
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersで検索フィルターが大文字小文字を区別しない場合") {
            then("検索フィルターが大文字小文字を区別しない") {
                val programs = listOf(
                    createProgramWithWork(title = "テスト作品A"),
                    createProgramWithWork(title = "テスト作品a"),
                    createProgramWithWork(title = "作品")
                )
                val filterState = FilterState(searchQuery = "A")
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
                result.all { it.work.title.contains("テスト") } shouldBe true
            }
        }
        `when`("applyFiltersで放送済みフィルターを適用した場合") {
            then("放送済みフィルターが正しく機能する") {
                val now = LocalDateTime.now()
                val programs = listOf(
                    createProgramWithWork(startedAt = now.minusHours(1)), // 放送済み
                    createProgramWithWork(startedAt = now.plusHours(1))   // 未放送
                )
                val filterState = FilterState(showOnlyAired = true)
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].firstProgram.startedAt shouldBe now.minusHours(1)
            }
            then("放送済みフィルターが無効の場合は全てのプログラムを返す") {
                val now = LocalDateTime.now()
                val programs = listOf(
                    createProgramWithWork(startedAt = now.minusHours(1)),
                    createProgramWithWork(startedAt = now.plusHours(1))
                )
                val filterState = FilterState(showOnlyAired = false)
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 2
            }
        }
        `when`("applyFiltersで放送開始時間の昇順ソートを適用した場合") {
            then("放送開始時間の昇順ソートが正しく機能する") {
                val now = LocalDateTime.now()
                val programs = listOf(
                    createProgramWithWork(startedAt = now.plusHours(2)),
                    createProgramWithWork(startedAt = now),
                    createProgramWithWork(startedAt = now.plusHours(1))
                )
                val filterState = FilterState(sortOrder = SortOrder.START_TIME_ASC, showOnlyAired = false)
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 3
                result[0].firstProgram.startedAt shouldBe now
                result[1].firstProgram.startedAt shouldBe now.plusHours(1)
                result[2].firstProgram.startedAt shouldBe now.plusHours(2)
            }
        }
        `when`("applyFiltersで放送開始時間の降順ソートを適用した場合") {
            then("放送開始時間の降順ソートが正しく機能する") {
                val now = LocalDateTime.now()
                val programs = listOf(
                    createProgramWithWork(startedAt = now),
                    createProgramWithWork(startedAt = now.plusHours(2)),
                    createProgramWithWork(startedAt = now.plusHours(1))
                )
                val filterState = FilterState(sortOrder = SortOrder.START_TIME_DESC, showOnlyAired = false)
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 3
                result[0].firstProgram.startedAt shouldBe now.plusHours(2)
                result[1].firstProgram.startedAt shouldBe now.plusHours(1)
                result[2].firstProgram.startedAt shouldBe now
            }
        }
        `when`("applyFiltersで複数のフィルターを組み合わせた場合") {
            then("複数のフィルターが正しく機能する") {
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
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].work.seasonName shouldBe SeasonName.WINTER
                result[0].work.media shouldBe "TV"
                result[0].firstProgram.channel.name shouldBe "TOKYO MX"
                result[0].work.viewerStatusState shouldBe StatusState.WATCHING
            }
        }
        `when`("applyFiltersで空のリストを渡した場合") {
            then("空のリストを返す") {
                val programs = emptyList<ProgramWithWork>()
                val filterState = FilterState(
                    selectedSeason = setOf(SeasonName.WINTER),
                    selectedMedia = setOf("TV")
                )
                val result = programFilter.applyFilters(programs, filterState)
                result shouldBe emptyList()
            }
        }
        `when`("applyFiltersでフィルターの順序が結果に影響を与えない場合") {
            then("フィルターの順序が結果に影響を与えない") {
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
                val result1 = programFilter.applyFilters(programs, filterState1)
                val result2 = programFilter.applyFilters(programs, filterState2)
                result1.size shouldBe result2.size
                result1.map { it.work.seasonName } shouldBe result2.map { it.work.seasonName }
                result1.map { it.work.media } shouldBe result2.map { it.work.media }
            }
        }
        `when`("applyFiltersで特殊文字を含む検索クエリを渡した場合") {
            then("特殊文字を含む検索クエリが正しく機能する") {
                val programs = listOf(
                    createProgramWithWork(title = "テスト作品!"),
                    createProgramWithWork(title = "テスト作品？"),
                    createProgramWithWork(title = "テスト作品")
                )
                val filterState = FilterState(searchQuery = "テスト作品!")
                val result = programFilter.applyFilters(programs, filterState)
                result.size shouldBe 1
                result[0].work.title shouldBe "テスト作品!"
            }
        }
    }

})