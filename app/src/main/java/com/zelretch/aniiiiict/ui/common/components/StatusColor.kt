package com.zelretch.aniiiiict.ui.common.components

import androidx.compose.ui.graphics.Color
import com.annict.type.StatusState
import com.zelretch.aniiiiict.ui.theme.CompletedBlue
import com.zelretch.aniiiiict.ui.theme.DroppedRed
import com.zelretch.aniiiiict.ui.theme.OnHoldPurple
import com.zelretch.aniiiiict.ui.theme.PlanToWatchOrange
import com.zelretch.aniiiiict.ui.theme.WatchingGreen

/**
 * ステータスごとのセマンティックカラー（ライブラリ・作品詳細・放送スケジュールで共通利用）。
 * 色は ui/theme/Color.kt の既存トークンを流用する。
 */
fun StatusState.toStatusColor(): Color = when (this) {
    StatusState.WATCHING -> WatchingGreen
    StatusState.WANNA_WATCH -> PlanToWatchOrange
    StatusState.WATCHED -> CompletedBlue
    StatusState.ON_HOLD -> OnHoldPurple
    StatusState.STOP_WATCHING -> DroppedRed
    else -> WatchingGreen
}
