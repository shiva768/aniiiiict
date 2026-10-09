package com.zelretch.aniiiiict.ui.common.components

import com.zelretch.aniiiiict.data.model.WorkPriority

fun WorkPriority.toJapaneseLabel(): String = when (this) {
    WorkPriority.FEATURED -> "注目"
    WorkPriority.NORMAL -> "ふつう"
    WorkPriority.DEFERRED -> "後回し"
}
