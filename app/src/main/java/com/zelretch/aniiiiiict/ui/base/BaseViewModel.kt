package com.zelretch.aniiiiiict.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * 共通のローディング処理を提供する基底ViewModelクラス
 */
abstract class BaseViewModel : ViewModel() {
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

                // 処理の実行と最小ローディング時間を並行実行
                val minLoadingTimeTask = async { delay(1000) }
                val blockTask = async { block() }
                
                // 処理の完了を待つ
                blockTask.await()
                
                // 最小ローディング時間の完了も待つ
                minLoadingTimeTask.await()
            } catch (e: Exception) {
                // エラーを設定
                Timber.e(e, "[%s][ローディング処理中にエラーが発生] %s", "BaseViewModel", e.message ?: "Unknown error")
                updateErrorState(e.message ?: "処理中にエラーが発生しました")
            } finally {
                // ローディング状態を終了
                updateLoadingState(false)
            }
        }
    }
}
