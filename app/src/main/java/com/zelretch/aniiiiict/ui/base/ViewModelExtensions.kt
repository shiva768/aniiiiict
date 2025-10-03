package com.zelretch.aniiiiict.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * ViewModelに対する便利な拡張関数群
 *
 * Now in Android パターンで共通的に使用される処理を提供する。
 */

/**
 * 最小ローディング時間を保証してコルーチンを起動する
 *
 * ユーザー体験向上のため、処理が早く終わった場合でも
 * 指定時間はローディング表示を維持する。
 * これにより、画面のチラつきを防ぐ。
 *
 * @param minLoadingTimeMillis 最小ローディング時間（ミリ秒、デフォルト: 1000ms）
 * @param context コルーチンコンテキスト（オプション）
 * @param block 実行する処理
 * @return 起動されたJob
 *
 * ## 使用例
 * ```kotlin
 * fun loadData() {
 *     launchWithMinLoadingTime {
 *         _uiState.value = UiState.Loading
 *         val result = repository.getData()
 *         _uiState.value = UiState.Success(result)
 *     }
 * }
 * ```
 */
fun ViewModel.launchWithMinLoadingTime(
    minLoadingTimeMillis: Long = 1000L,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job = viewModelScope.launch(context) {
    val startTime = System.currentTimeMillis()

    // メイン処理を実行
    block()

    // 経過時間を計算
    val elapsedTime = System.currentTimeMillis() - startTime

    // 最小時間に達していない場合は待機
    if (elapsedTime < minLoadingTimeMillis) {
        delay(minLoadingTimeMillis - elapsedTime)
    }
}

/**
 * UiStateを更新しながらコルーチンを起動する便利な拡張関数
 *
 * Loading → Success/Error の流れを簡潔に記述できる。
 * ErrorMapperを使用してエラーメッセージを自動変換する。
 *
 * @param T データ型
 * @param errorMapper エラーをメッセージに変換するマッパー
 * @param updateState UiStateを更新する関数
 * @param context コルーチンコンテキスト（オプション）
 * @param minLoadingTimeMillis 最小ローディング時間（オプション、null = 使用しない）
 * @param block データを取得する処理（Result<T>を返す）
 * @return 起動されたJob
 *
 * ## 使用例
 * ```kotlin
 * fun loadAnimeDetail(program: ProgramWithWork) {
 *     launchWithUiState(
 *         errorMapper = errorMapper,
 *         updateState = { _uiState.value = it }
 *     ) {
 *         getAnimeDetailUseCase(program)
 *     }
 * }
 * ```
 */
fun <T> ViewModel.launchWithUiState(
    errorMapper: ErrorMapper,
    updateState: (UiState<T>) -> Unit,
    context: CoroutineContext = EmptyCoroutineContext,
    minLoadingTimeMillis: Long? = 1000L,
    block: suspend () -> Result<T>
): Job {
    val launcher: (suspend CoroutineScope.() -> Unit) -> Job = { operation ->
        if (minLoadingTimeMillis != null) {
            launchWithMinLoadingTime(minLoadingTimeMillis, context, operation)
        } else {
            viewModelScope.launch(context, block = operation)
        }
    }

    return launcher {
        // ローディング状態に設定
        updateState(UiState.Loading)

        // 処理を実行
        val result = block()

        // 結果に応じてStateを更新
        result
            .onSuccess { data ->
                updateState(UiState.Success(data))
            }
            .onFailure { error ->
                val message = errorMapper.toUserMessage(error)
                updateState(UiState.Error(message))
            }
    }
}
