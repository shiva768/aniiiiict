package com.zelretch.aniiiiict.ui.common

import androidx.lifecycle.ViewModel
import com.zelretch.aniiiiict.data.api.AnnictStatusMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Annict の障害状態を UI に公開する。全画面共通の障害バナー用。
 */
@HiltViewModel
class AnnictStatusViewModel @Inject constructor(monitor: AnnictStatusMonitor) : ViewModel() {
    val isAnnictDown: StateFlow<Boolean> = monitor.isDown
}
