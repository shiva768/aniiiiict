package com.zelretch.aniiiiiict.ui.base

import kotlinx.coroutines.flow.StateFlow

/**
 * 基本的なViewModelの契約を定義するインターフェース
 * テスト容易性向上のため、ViewModelの公開APIを定義
 */
interface ViewModelContract<T : BaseUiState> {
    /**
     * UI状態のStateFlow
     */
    val uiState: StateFlow<T>
    
    /**
     * エラーをクリアする
     */
    fun clearError()
}