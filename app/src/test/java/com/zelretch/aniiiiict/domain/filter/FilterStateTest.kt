package com.zelretch.aniiiiict.domain.filter

import com.annict.type.SeasonName
import com.annict.type.StatusState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FilterState")
class FilterStateTest {

    @Nested
    @DisplayName("初期化")
    inner class Initialization {

        @Test
        @DisplayName("デフォルト値が正しく設定される")
        fun デフォルト値が正しく設定される() {
            // When
            val filterState = FilterState()

            // Then
            assertEquals(emptySet<String>(), filterState.selectedMedia)
            assertEquals(emptySet<SeasonName>(), filterState.selectedSeason)
            assertEquals(emptySet<Int>(), filterState.selectedYear)
            assertEquals(emptySet<String>(), filterState.selectedChannel)
            assertEquals(emptySet<StatusState>(), filterState.selectedStatus)
            assertEquals("", filterState.searchQuery)
            assertEquals(true, filterState.showOnlyAired)
            assertEquals(SortOrder.START_TIME_ASC, filterState.sortOrder)
        }
    }

    @Nested
    @DisplayName("コピー")
    inner class Copy {

        @Test
        @DisplayName("一部のプロパティが変更される")
        fun 一部のプロパティが変更される() {
            // Given
            val original = FilterState()

            // When
            val modified = original.copy(
                selectedMedia = setOf("TV"),
                searchQuery = "テスト"
            )

            // Then
            assertEquals(setOf("TV"), modified.selectedMedia)
            assertEquals("テスト", modified.searchQuery)
            assertEquals(emptySet<SeasonName>(), modified.selectedSeason)
            assertEquals(emptySet<Int>(), modified.selectedYear)
            assertEquals(emptySet<String>(), modified.selectedChannel)
        }

        @Test
        @DisplayName("プロパティを変更せずにコピーすると元と等しい")
        fun プロパティを変更せずにコピーすると元と等しい() {
            // Given
            val original = FilterState(
                selectedMedia = setOf("TV"),
                selectedSeason = setOf(SeasonName.WINTER),
                selectedYear = setOf(2023),
                selectedChannel = setOf("TOKYO MX"),
                selectedStatus = setOf(StatusState.WATCHING),
                searchQuery = "テスト",
                showOnlyAired = true,
                sortOrder = SortOrder.START_TIME_ASC
            )

            // When
            val copy = original.copy()

            // Then
            assertEquals(original, copy)
        }
    }

    @Nested
    @DisplayName("等価性")
    inner class Equality {

        @Test
        @DisplayName("2つの空の状態は等しい")
        fun 空の状態同士は等しい() {
            // Given
            val state1 = FilterState()
            val state2 = FilterState()

            // Then
            assertEquals(state1, state2)
        }

        @Test
        @DisplayName("異なる状態は等しくない")
        fun 異なる状態は等しくない() {
            // Given
            val state1 = FilterState()
            val state3 = FilterState(selectedMedia = setOf("TV"))

            // Then
            assertNotEquals(state1, state3)
        }
    }

    @Nested
    @DisplayName("不変性")
    inner class Immutability {

        @Test
        @DisplayName("プロパティ変更時に元の状態とは等しくない")
        fun プロパティ変更時に元の状態とは等しくない() {
            // Given
            val original = FilterState(
                selectedMedia = setOf("TV"),
                selectedSeason = setOf(SeasonName.WINTER),
                selectedYear = setOf(2023),
                selectedChannel = setOf("TOKYO MX"),
                selectedStatus = setOf(StatusState.WATCHING),
                searchQuery = "テスト",
                showOnlyAired = true,
                sortOrder = SortOrder.START_TIME_ASC
            )

            // When
            val modified = original.copy(selectedMedia = setOf("OVA"))

            // Then
            assertNotEquals(original, modified)
        }
    }
}
