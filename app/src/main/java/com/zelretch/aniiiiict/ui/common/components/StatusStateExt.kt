package com.zelretch.aniiiiict.ui.common.components

import com.annict.type.StatusState

fun StatusState.toJapaneseLabel(): String = when (this) {
    StatusState.WATCHING -> "視聴中"
    StatusState.WANNA_WATCH -> "見たい"
    StatusState.WATCHED -> "見た"
    StatusState.STOP_WATCHING -> "中止"
    StatusState.ON_HOLD -> "保留"
    StatusState.UNKNOWN__ -> "未設定"
    else -> toString()
}
