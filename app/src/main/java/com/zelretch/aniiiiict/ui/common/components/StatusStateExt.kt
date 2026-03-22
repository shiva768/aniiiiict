package com.zelretch.aniiiiict.ui.common.components

import com.annict.type.StatusState

fun StatusState.toJapaneseLabel(): String = when (this) {
    StatusState.WATCHING -> "見てる"
    StatusState.WANNA_WATCH -> "見たい"
    StatusState.WATCHED -> "見た"
    StatusState.STOP_WATCHING -> "中止"
    StatusState.ON_HOLD -> "中断"
    StatusState.UNKNOWN__ -> "未設定"
    else -> toString()
}
