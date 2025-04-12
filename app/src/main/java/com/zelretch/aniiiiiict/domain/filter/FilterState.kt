package com.zelretch.aniiiiiict.domain.filter

import com.zelretch.aniiiiiict.type.StatusState

data class FilterState(
    val selectedMedia: String? = null,
    val selectedSeason: String? = null,
    val selectedYear: Int? = null,
    val selectedChannel: String? = null,
    val selectedStatus: StatusState? = null,
    val searchQuery: String = ""
) 