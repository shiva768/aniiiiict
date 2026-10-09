package com.zelretch.aniiiiict.ui.common.components

import com.zelretch.aniiiiict.data.model.WorkPriority

fun WorkPriority.toJapaneseLabel(): String = when (this) {
    WorkPriority.TIER1 -> "Tier1"
    WorkPriority.TIER2 -> "Tier2"
    WorkPriority.TIER3 -> "Tier3"
    WorkPriority.NONE -> "無印"
}
