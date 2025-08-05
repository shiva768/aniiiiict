package com.zelretch.aniiiiiict.domain.filter

import com.annict.type.SeasonName
import com.annict.type.StatusState

data class FilterState(
    val selectedMedia: Set<String> = emptySet(),
    val selectedSeason: Set<SeasonName> = emptySet(),
    val selectedYear: Set<Int> = emptySet(),
    val selectedChannel: Set<String> = emptySet(),
    val selectedStatus: Set<StatusState> = emptySet(),
    val searchQuery: String = "",
    val showOnlyAired: Boolean = true,
    val sortOrder: SortOrder = SortOrder.START_TIME_DESC
)

enum class SortOrder {
    START_TIME_ASC,  // 放送開始時間（昇順）
    START_TIME_DESC  // 放送開始時間（降順）
} 