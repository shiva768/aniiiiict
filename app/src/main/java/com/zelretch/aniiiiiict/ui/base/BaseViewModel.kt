package com.zelretch.aniiiiiict.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiiict.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 共通のローディング処理を提供する基底ViewModelクラス
 */
abstract class BaseViewModel(val logger: Logger) : ViewModel() {
    private val TAG = "BaseViewModel"

    /**
     * ローディング状態を更新する関数
     */
    abstract fun updateLoadingState(isLoading: Boolean)

    /**
     * エラー状態を更新する関数
     */
    abstract fun updateErrorState(error: String?)

    /**
     * ローディング処理を実行する
     *
     * @param block 実際に実行する処理
     */
    protected fun executeWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                // ローディング状態を開始
                updateLoadingState(true)
                updateErrorState(null)

                // 処理を実行
                val startTime = System.currentTimeMillis()
                block()

                // 最小限のローディング時間を確保（1秒）
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < 1000) {
                    delay(1000 - elapsedTime)
                }
            } catch (e: Exception) {
                // エラーを設定
                logger.logError(TAG, e, "ローディング処理中にエラーが発生")
                updateErrorState(e.message ?: "処理中にエラーが発生しました")
            } finally {
                // ローディング状態を終了
                updateLoadingState(false)
            }
        }
    }
} 