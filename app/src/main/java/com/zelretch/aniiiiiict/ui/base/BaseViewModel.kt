package com.zelretch.aniiiiiict.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * 共通のローディング処理を提供する基底ViewModelクラス
 */
abstract class BaseViewModel : ViewModel() {

    private val loadingCounter = AtomicInteger(0)

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
     * @return The job that was launched.
     */
    protected fun executeWithLoading(block: suspend () -> Unit): Job = viewModelScope.launch {
        if (loadingCounter.getAndIncrement() == 0) {
            updateLoadingState(true)
        }
        try {
            updateErrorState(null)

            // 最小ローディング時間のジョブを開始
            val loadingJob = launch {
                delay(1000) // Minimum loading time
            }

            // 処理を実行
            block()

            // 最小ローディング時間の完了を待つ
            loadingJob.join()
        } catch (e: Exception) {
            // エラーを設定
            Timber.e(e, "[%s][ローディング処理中にエラーが発生] %s", "BaseViewModel", e.message ?: "Unknown error")
            updateErrorState(e.message ?: "処理中にエラーが発生しました")
        } finally {
            // ローディング状態を終了
            if (loadingCounter.decrementAndGet() == 0) {
                updateLoadingState(false)
            }
        }
    }
}
