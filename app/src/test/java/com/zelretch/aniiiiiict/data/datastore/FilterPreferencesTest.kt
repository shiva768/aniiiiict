package com.zelretch.aniiiiiict.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FilterPreferencesTest : BehaviorSpec({
    val context = ApplicationProvider.getApplicationContext<Context>()
    val filterPreferences = FilterPreferences(context)

    afterSpec { runBlocking { filterPreferences.updateFilterState(FilterState()) } }

    given("FilterPreferences") {
        `when`("フィルター状態が保存される") {
            then("正しく復元されること") {
                runBlocking {
                    val filterState = FilterState(selectedMedia = setOf("TV"), sortOrder = SortOrder.ALPHABETICAL)
                    filterPreferences.updateFilterState(filterState)
                    val restoredState = filterPreferences.getFilterState().first()
                    restoredState shouldBe filterState
                }
            }
        }

        `when`("空のフィルター状態が保存される") {
            then("正しく復元されること") {
                runBlocking {
                    val emptyFilterState = FilterState()
                    filterPreferences.updateFilterState(emptyFilterState)
                    val restoredState = filterPreferences.getFilterState().first()
                    restoredState shouldBe emptyFilterState
                }
            }
        }

        `when`("不正な値が含まれる場合") {
            then("正しく復元されること") {
                runBlocking {
                    val invalidFilterState = FilterState(selectedMedia = setOf("INVALID_MEDIA"))
                    filterPreferences.updateFilterState(invalidFilterState)
                    val restoredState = filterPreferences.getFilterState().first()
                    restoredState shouldBe invalidFilterState
                }
            }
        }

        `when`("並行処理での更新が行われる") {
            then("正しく行われること") {
                runBlocking {
                    val initialFilterState = FilterState(selectedMedia = setOf("TV"))
                    filterPreferences.updateFilterState(initialFilterState)

                    // 並行処理での更新
                    launch {
                        filterPreferences.updateFilterState(FilterState(selectedMedia = setOf("MOVIE")))
                    }

                    val restoredState = filterPreferences.getFilterState().first()
                    restoredState.selectedMedia shouldBe setOf("MOVIE")
                }
            }
        }
    }
})