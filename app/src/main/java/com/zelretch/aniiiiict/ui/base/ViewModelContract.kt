package com.zelretch.aniiiiict.ui.base

import kotlinx.coroutines.flow.StateFlow

/**
 * 基本的なViewModelの契約を定義するインターフェース
 * テスト容易性向上のため、ViewModelの公開APIを定義
 *
 * Note: Now in Android パターンへの移行に伴い、BaseUiStateの制約を削除。
 * 各ViewModelは独自のUiState型を定義できる。
 */
interface ViewModelContract<T> {
    /**
     * UI状態のStateFlow
     */
    val uiState: StateFlow<T>

    /**
     * エラーをクリアする
     */
    fun clearError()
}
