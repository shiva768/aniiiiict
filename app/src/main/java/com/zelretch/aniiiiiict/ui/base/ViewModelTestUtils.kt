package com.zelretch.aniiiiiict.ui.base

import com.zelretch.aniiiiiict.ui.base.BaseUiState

/**
 * ViewModelテスト用のユーティリティクラス
 * テスト容易性向上のために共通的なテスト機能を提供
 */
object ViewModelTestUtils {
    
    /**
     * TestableViewModelインターフェースを実装したViewModelに対して
     * エラー状態を簡単に設定するヘルパー関数
     */
    inline fun <reified T : BaseUiState> TestableViewModel<T>.setErrorState(error: String) {
        setErrorForTest(error)
    }
    
    /**
     * TestableViewModelインターフェースを実装したViewModelに対して
     * ローディング状態を簡単に設定するヘルパー関数
     */
    inline fun <reified T : BaseUiState> TestableViewModel<T>.setLoadingState(isLoading: Boolean) {
        setLoadingForTest(isLoading)
    }
    
    /**
     * ViewModelの状態を初期状態にリセットするヘルパー関数
     */
    inline fun <reified T : BaseUiState> TestableViewModel<T>.resetToInitialState() {
        setLoadingForTest(false)
        setErrorForTest(null)
    }
}

/**
 * Hiltテスト用のアノテーションのガイド
 * 
 * テスト容易性向上のため、以下のHiltテスト機能を活用することを推奨:
 * 
 * 1. @UninstallModules: プロダクションモジュールを削除してテスト用モジュールと差し替え
 *    例: @UninstallModules(RepositoryModule::class)
 * 
 * 2. @BindValue: テスト用の依存関係を直接注入
 *    例: @BindValue @JvmField val repository: TestRepository = mockk()
 * 
 * 3. @TestInstallIn: テスト専用のモジュールをインストール
 *    例: @TestInstallIn(component = SingletonComponent::class, replaces = [RepositoryModule::class])
 */