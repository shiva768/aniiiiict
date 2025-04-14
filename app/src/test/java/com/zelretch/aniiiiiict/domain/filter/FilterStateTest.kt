package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterStateTest {
    @Test
    fun `デフォルト値が正しく設定されている`() {
        val filterState = FilterState()

        assertTrue(filterState.selectedMedia.isEmpty())
        assertTrue(filterState.selectedSeason.isEmpty())
        assertTrue(filterState.selectedYear.isEmpty())
        assertTrue(filterState.selectedChannel.isEmpty())
        assertTrue(filterState.selectedStatus.isEmpty())
        assertEquals("", filterState.searchQuery)
        assertTrue(filterState.showOnlyAired)
        assertEquals(SortOrder.START_TIME_DESC, filterState.sortOrder)
    }

    @Test
    fun `すべてのプロパティを設定した場合に正しく保持される`() {
        val filterState = FilterState(
            selectedMedia = setOf("TV", "OVA"),
            selectedSeason = setOf(SeasonName.WINTER, SeasonName.SUMMER),
            selectedYear = setOf(2023, 2024),
            selectedChannel = setOf("TOKYO MX", "AT-X"),
            selectedStatus = setOf(StatusState.WATCHING, StatusState.WANNA_WATCH),
            searchQuery = "テスト",
            showOnlyAired = false,
            sortOrder = SortOrder.START_TIME_ASC
        )

        assertEquals(setOf("TV", "OVA"), filterState.selectedMedia)
        assertEquals(setOf(SeasonName.WINTER, SeasonName.SUMMER), filterState.selectedSeason)
        assertEquals(setOf(2023, 2024), filterState.selectedYear)
        assertEquals(setOf("TOKYO MX", "AT-X"), filterState.selectedChannel)
        assertEquals(setOf(StatusState.WATCHING, StatusState.WANNA_WATCH), filterState.selectedStatus)
        assertEquals("テスト", filterState.searchQuery)
        assertTrue(!filterState.showOnlyAired)
        assertEquals(SortOrder.START_TIME_ASC, filterState.sortOrder)
    }

    @Test
    fun `copyメソッドで一部のプロパティのみを変更できる`() {
        val original = FilterState()
        val modified = original.copy(
            selectedMedia = setOf("TV"),
            searchQuery = "テスト"
        )

        assertEquals(setOf("TV"), modified.selectedMedia)
        assertEquals("テスト", modified.searchQuery)
        // 他のプロパティは変更されていない
        assertTrue(modified.selectedSeason.isEmpty())
        assertTrue(modified.selectedYear.isEmpty())
        assertTrue(modified.selectedChannel.isEmpty())
        assertTrue(modified.selectedStatus.isEmpty())
        assertTrue(modified.showOnlyAired)
        assertEquals(SortOrder.START_TIME_DESC, modified.sortOrder)
    }

    @Test
    fun `等価性が正しく判定される`() {
        val state1 = FilterState(
            selectedMedia = setOf("TV"),
            searchQuery = "テスト"
        )
        val state2 = FilterState(
            selectedMedia = setOf("TV"),
            searchQuery = "テスト"
        )
        val state3 = FilterState(
            selectedMedia = setOf("OVA"),
            searchQuery = "テスト"
        )

        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    @Test
    fun `空のFilterState同士は等しい`() {
        val state1 = FilterState()
        val state2 = FilterState()

        assertEquals(state1, state2)
    }

    @Test
    fun `すべてのSetプロパティの不変性が保証されている`() {
        // 初期のSetを作成
        val initialMedia = setOf("TV")
        val initialSeason = setOf(SeasonName.WINTER)
        val initialYear = setOf(2023)
        val initialChannel = setOf("TOKYO MX")
        val initialStatus = setOf(StatusState.WATCHING)

        // FilterStateを作成
        val filterState = FilterState(
            selectedMedia = initialMedia,
            selectedSeason = initialSeason,
            selectedYear = initialYear,
            selectedChannel = initialChannel,
            selectedStatus = initialStatus
        )

        // 各Setプロパティが初期値と一致することを確認
        assertEquals(initialMedia, filterState.selectedMedia)
        assertEquals(initialSeason, filterState.selectedSeason)
        assertEquals(initialYear, filterState.selectedYear)
        assertEquals(initialChannel, filterState.selectedChannel)
        assertEquals(initialStatus, filterState.selectedStatus)

        // 新しいSetを作成して変更を試みる
        val newMediaSet = filterState.selectedMedia.toMutableSet().apply { add("OVA") }
        val newSeasonSet =
            filterState.selectedSeason.toMutableSet().apply { add(SeasonName.SUMMER) }
        val newYearSet = filterState.selectedYear.toMutableSet().apply { add(2024) }
        val newChannelSet = filterState.selectedChannel.toMutableSet().apply { add("AT-X") }
        val newStatusSet =
            filterState.selectedStatus.toMutableSet().apply { add(StatusState.WANNA_WATCH) }

        // 元のSetは変更されていないことを確認
        assertEquals(initialMedia, filterState.selectedMedia)
        assertEquals(initialSeason, filterState.selectedSeason)
        assertEquals(initialYear, filterState.selectedYear)
        assertEquals(initialChannel, filterState.selectedChannel)
        assertEquals(initialStatus, filterState.selectedStatus)

        // 新しいSetは変更されていることを確認
        assertEquals(setOf("TV", "OVA"), newMediaSet)
        assertEquals(setOf(SeasonName.WINTER, SeasonName.SUMMER), newSeasonSet)
        assertEquals(setOf(2023, 2024), newYearSet)
        assertEquals(setOf("TOKYO MX", "AT-X"), newChannelSet)
        assertEquals(setOf(StatusState.WATCHING, StatusState.WANNA_WATCH), newStatusSet)
    }
} 