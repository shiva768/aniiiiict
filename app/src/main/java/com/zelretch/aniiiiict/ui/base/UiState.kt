package com.zelretch.aniiiiict.ui.base

/**
 * UI状態を表すsealed interface
 *
 * Now in Android パターンに準拠した、統一されたUI状態管理を提供する。
 * 全てのViewModelで同じパターンを使用することで、一貫性とテスタビリティを向上させる。
 *
 * @param T データ型（Success時に保持するデータの型）
 *
 * @see <a href="https://developer.android.com/topic/architecture/ui-layer/stateholders#ui-state">
 *   UI State documentation
 * </a>
 * @see <a href="https://github.com/android/nowinandroid">Now in Android sample</a>
 */
sealed interface UiState<out T> {

    /**
     * ローディング中の状態
     *
     * 初期ロード時や、データ取得中に使用する。
     */
    data object Loading : UiState<Nothing>

    /**
     * データ取得成功状態
     *
     * @param data 取得したデータ
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * エラー状態
     *
     * @param message ユーザーに表示するエラーメッセージ
     */
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * UiState がローディング中かどうかを判定する拡張プロパティ
 */
val UiState<*>.isLoading: Boolean
    get() = this is UiState.Loading

/**
 * UiState がエラー状態かどうかを判定する拡張プロパティ
 */
val UiState<*>.isError: Boolean
    get() = this is UiState.Error

/**
 * UiState が成功状態かどうかを判定する拡張プロパティ
 */
val UiState<*>.isSuccess: Boolean
    get() = this is UiState.Success

/**
 * UiState.Success からデータを安全に取得する拡張関数
 *
 * @return Success状態の場合はデータ、それ以外はnull
 */
fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

/**
 * UiState.Error からエラーメッセージを安全に取得する拡張関数
 *
 * @return Error状態の場合はメッセージ、それ以外はnull
 */
fun UiState<*>.getErrorOrNull(): String? = when (this) {
    is UiState.Error -> message
    else -> null
}
