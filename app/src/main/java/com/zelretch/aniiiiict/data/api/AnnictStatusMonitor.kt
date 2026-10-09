package com.zelretch.aniiiiict.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Annict の稼働状況を保持する。
 * [AnnictHealthInterceptor] が Annict への通信結果を報告し、UI はこれを購読して障害バナーを出す。
 * 正常な応答が1度でも返れば復旧とみなす。
 */
@Singleton
class AnnictStatusMonitor @Inject constructor() {

    private val _isDown = MutableStateFlow(false)
    val isDown: StateFlow<Boolean> = _isDown.asStateFlow()

    fun reportOutage(reason: String) {
        if (!_isDown.value) Timber.w("Annict の障害を検知: $reason")
        _isDown.value = true
    }

    fun reportHealthy() {
        if (_isDown.value) Timber.i("Annict の復旧を検知")
        _isDown.value = false
    }
}
