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

/**
 * テスト時のUI状態操作を可能にするインターフェース
 * プロダクションコードでは使用せず、テスト時にのみ使用する
 */
interface TestableViewModel<T : BaseUiState> {
    /**
     * テスト用: UI状態を直接設定する
     * @param state 設定するUI状態
     */
    fun setUiStateForTest(state: T)
    
    /**
     * テスト用: エラー状態を設定する
     * @param error エラーメッセージ
     */
    fun setErrorForTest(error: String?)
    
    /**
     * テスト用: ローディング状態を設定する
     * @param isLoading ローディング状態
     */
    fun setLoadingForTest(isLoading: Boolean)
}