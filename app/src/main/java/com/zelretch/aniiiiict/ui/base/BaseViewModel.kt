package com.zelretch.aniiiiict.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 共通のローディング処理を提供する基底ViewModelクラス
 */
abstract class BaseViewModel : ViewModel() {

    companion object {
        private const val MINIMUM_LOADING_TIME_MS = 1000L
    }

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
                delay(MINIMUM_LOADING_TIME_MS) // Minimum loading time
            }

            // 処理を実行
            block()

            // 最小ローディング時間の完了を待つ
            loadingJob.join()
        } catch (e: CancellationException) {
            // キャンセルは上位に伝播させる（ローディング状態は finally で解消）
            throw e
        } catch (e: Exception) {
            // 統一されたエラーハンドリング（ユーザー向けメッセージを設定）
            val errorMessage = ErrorHandler.handleError(e, "BaseViewModel", "executeWithLoading")
            updateErrorState(errorMessage)
        } finally {
            // ローディング状態を終了
            if (loadingCounter.decrementAndGet() == 0) {
                updateLoadingState(false)
            }
        }
    }
}
