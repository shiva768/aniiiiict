package com.zelretch.aniiiiict.ui

import com.zelretch.aniiiiict.MainUiState
import com.zelretch.aniiiiict.ui.base.ViewModelContract

/**
 * MainViewModelの契約を定義するインターフェース
 * テスト容易性向上のため、ViewModelの公開APIを定義
 */
interface MainViewModelContract : ViewModelContract<MainUiState> {
    /**
     * 認証を開始する
     */
    fun startAuth()

    /**
     * 認証コールバックを処理する
     * @param code 認証コード
     */
    fun handleAuthCallback(code: String?)

    /**
     * 認証状態を手動で確認する
     */
    fun checkAuthentication()

    /**
     * 認証をキャンセルする（ブラウザを閉じた場合など）
     */
    fun cancelAuth()
}
