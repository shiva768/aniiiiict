package com.zelretch.aniiiiict.domain.filter

import com.annict.type.SeasonName
import com.annict.type.StatusState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class フィルター状態テスト : BehaviorSpec({
    given("新しいFilterState") {
        val filterState = FilterState()

        `when`("初期化されたとき") {
            then("デフォルト値が正しく設定されているべき") {
                filterState.selectedMedia shouldBe emptySet()
                filterState.selectedSeason shouldBe emptySet()
                filterState.selectedYear shouldBe emptySet()
                filterState.selectedChannel shouldBe emptySet()
                filterState.selectedStatus shouldBe emptySet()
                filterState.searchQuery shouldBe ""
                filterState.showOnlyAired shouldBe true
                filterState.sortOrder shouldBe SortOrder.START_TIME_DESC
            }
        }
    }

    given("コピー方法") {
        val original = FilterState()
        val modified = original.copy(
            selectedMedia = setOf("TV"),
            searchQuery = "テスト"
        )

        `when`("一部のプロパティが変更されたとき") {
            then("変更が反映されるべき") {
                modified.selectedMedia shouldBe setOf("TV")
                modified.searchQuery shouldBe "テスト"
                modified.selectedSeason shouldBe emptySet()
                modified.selectedYear shouldBe emptySet()
                modified.selectedChannel shouldBe emptySet()
            }
        }
    }

    given("等価性チェック") {
        val state1 = FilterState()
        val state2 = FilterState()
        val state3 = FilterState(selectedMedia = setOf("TV"))

        `when`("2つの空の状態を比較したとき") {
            then("等しいべき") {
                state1 shouldBe state2
            }
        }

        `when`("異なる状態を比較したとき") {
            then("等しくないべき") {
                state1 shouldNotBe state3
            }
        }
    }

    given("不変性チェック") {
        val original = FilterState(
            selectedMedia = setOf("TV"),
            selectedSeason = setOf(SeasonName.WINTER),
            selectedYear = setOf(2023),
            selectedChannel = setOf("TOKYO MX"),
            selectedStatus = setOf(StatusState.WATCHING),
            searchQuery = "テスト",
            showOnlyAired = true,
            sortOrder = SortOrder.START_TIME_DESC
        )

        `when`("プロパティを変更せずにコピーしたとき") {
            val copy = original.copy()
            then("元の状態と等しいべき") {
                copy shouldBe original
            }
        }

        `when`("プロパティを変更したとき") {
            val modified = original.copy(selectedMedia = setOf("OVA"))
            then("元の状態とは等しくないべき") {
                modified shouldNotBe original
            }
        }
    }
})
