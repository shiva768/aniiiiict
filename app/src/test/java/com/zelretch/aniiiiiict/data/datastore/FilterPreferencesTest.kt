package com.zelretch.aniiiiiict.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import com.zelretch.aniiiiiict.type.SeasonName
import com.zelretch.aniiiiiict.type.StatusState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilterPreferencesTest {
    private lateinit var context: Context
    private lateinit var filterPreferences: FilterPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        filterPreferences = FilterPreferences(context)
    }

    @After
    fun tearDown() = runBlocking {
        // テストデータのクリーンアップ
        filterPreferences.updateFilterState(FilterState())
    }

    @Test
    fun `フィルター状態が正しく保存され、復元される`() = runBlocking {
        // 準備
        val filterState = FilterState(
            selectedMedia = setOf("TV"),
            selectedSeason = setOf(SeasonName.WINTER),
            selectedYear = setOf(2023),
            selectedChannel = setOf("TOKYO MX"),
            selectedStatus = setOf(StatusState.WATCHING),
            searchQuery = "テスト",
            showOnlyAired = false,
            sortOrder = SortOrder.START_TIME_ASC
        )

        // 実行
        filterPreferences.updateFilterState(filterState)
        val restoredFilterState = filterPreferences.filterState.first()

        // 検証
        assertEquals(filterState, restoredFilterState)
    }

    @Test
    fun `空のフィルター状態が正しく保存され、復元される`() = runBlocking {
        // 準備
        val emptyFilterState = FilterState()

        // 実行
        filterPreferences.updateFilterState(emptyFilterState)
        val restoredFilterState = filterPreferences.filterState.first()

        // 検証
        assertEquals(emptyFilterState, restoredFilterState)
    }

    @Test
    fun `不正な値が含まれる場合でも正しく復元される`() = runBlocking {
        // 準備
        val filterState = FilterState(
            selectedMedia = setOf("TV", ""),
            selectedSeason = setOf(SeasonName.WINTER),
            selectedYear = setOf(2023, 0),
            selectedChannel = setOf("TOKYO MX", ""),
            selectedStatus = setOf(StatusState.WATCHING),
            searchQuery = "テスト",
            showOnlyAired = false,
            sortOrder = SortOrder.START_TIME_ASC
        )

        // 実行
        filterPreferences.updateFilterState(filterState)
        val restoredFilterState = filterPreferences.filterState.first()

        // 検証
        assertEquals(setOf("TV"), restoredFilterState.selectedMedia)
        assertEquals(setOf(SeasonName.WINTER), restoredFilterState.selectedSeason)
        assertEquals(setOf(2023), restoredFilterState.selectedYear)
        assertEquals(setOf("TOKYO MX"), restoredFilterState.selectedChannel)
        assertEquals(setOf(StatusState.WATCHING), restoredFilterState.selectedStatus)
    }

    @Test
    fun `並行処理での更新が正しく行われる`() = runBlocking {
        val jobs = List(10) { i ->
            launch {
                val filterState = FilterState(
                    selectedYear = setOf(2000 + i)
                )
                filterPreferences.updateFilterState(filterState)
            }
        }
        jobs.forEach { it.join() }
        val restoredFilterState = filterPreferences.filterState.first()
        assertTrue(restoredFilterState.selectedYear.isNotEmpty())
    }
} 