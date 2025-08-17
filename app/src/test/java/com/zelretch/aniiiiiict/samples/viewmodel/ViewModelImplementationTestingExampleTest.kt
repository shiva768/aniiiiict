package com.zelretch.aniiiiiict.samples.viewmodel

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import com.zelretch.aniiiiiict.MainUiState
import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import com.zelretch.aniiiiiict.testing.TestableViewModel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * ViewModelの実装テストの具体例
 * 
 * この例では3つのテストアプローチを示します：
 * 1. インターフェースベースのテスト（UI コンポーネント用）
 * 2. 実装テスト（ビジネスロジック用）  
 * 3. 統合テスト（エンドツーエンド用）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelImplementationTestingExampleTest : DescribeSpec({
    val testDispatcher = StandardTestDispatcher()

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    describe("ViewModelテストの3つのアプローチ") {

        context("1. インターフェースベースのテスト - UI コンポーネント用") {
            it("インターフェース経由でのテスト - 実装に依存しない") {
                // インターフェースを使用（実装詳細を隠蔽）
                val mockContract = mockk<MainViewModelContract>()
                val mockTestable = mockk<TestableViewModel<MainUiState>>()
                
                // UI状態を直接設定
                val testState = MainUiState(
                    isLoading = false,
                    error = null,
                    isAuthenticating = true,
                    isAuthenticated = false
                )
                
                every { mockContract.uiState.value } returns testState
                every { mockTestable.setUiStateForTest(any()) } just Runs
                every { mockContract.startAuth() } just Runs
                
                // UIコンポーネントのテスト
                mockContract.startAuth()
                mockTestable.setUiStateForTest(testState)
                
                // 動作確認
                verify { mockContract.startAuth() }
                verify { mockTestable.setUiStateForTest(testState) }
                mockContract.uiState.value.isAuthenticating shouldBe true
                
                println("✅ インターフェースベースのテスト: UI状態の操作が簡単")
            }
        }

        context("2. 実装テスト - ビジネスロジック用") {
            it("実際のViewModelの実装をテスト") {
                // 実際のViewModelインスタンスを作成
                val authUseCase = mockk<AnnictAuthUseCase>()
                val context = mockk<Context>()
                val customTabsIntent = mockk<CustomTabsIntent>(relaxUnitFun = true)
                val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
                
                every { customTabsIntentFactory.create() } returns customTabsIntent
                every { customTabsIntent.launchUrl(any(), any()) } just Runs
                coEvery { authUseCase.isAuthenticated() } returns false
                
                // 実際のViewModelを作成
                val viewModel = MainViewModel(
                    authUseCase, 
                    customTabsIntentFactory, 
                    context
                )
                
                // 初期状態のテスト
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticated shouldBe false
                
                // 認証開始のビジネスロジックをテスト
                coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"
                
                viewModel.startAuth()
                testDispatcher.scheduler.advanceUntilIdle()
                
                // 実装の詳細をテスト
                viewModel.uiState.value.isAuthenticating shouldBe true
                coVerify { authUseCase.getAuthUrl() }
                verify { customTabsIntent.launchUrl(context, any()) }
                
                println("✅ 実装テスト: 実際のビジネスロジックを検証")
            }
        }

        context("3. 統合テスト - エンドツーエンド用") {
            it("実際の認証フローをエンドツーエンドでテスト") {
                // 実際のViewModelと一部の実装を使用
                val authUseCase = mockk<AnnictAuthUseCase>()
                val context = mockk<Context>()
                val customTabsIntent = mockk<CustomTabsIntent>(relaxUnitFun = true)
                val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
                
                every { customTabsIntentFactory.create() } returns customTabsIntent
                every { customTabsIntent.launchUrl(any(), any()) } just Runs
                
                // 認証フローのシナリオを設定
                coEvery { authUseCase.isAuthenticated() } returns false
                coEvery { authUseCase.getAuthUrl() } returns "https://annict.jp/oauth/authorize?..."
                coEvery { authUseCase.handleAuthCallback("valid_code") } returns true
                
                val viewModel = MainViewModel(
                    authUseCase, 
                    customTabsIntentFactory, 
                    context
                )
                
                // フル認証フローのテスト
                testDispatcher.scheduler.advanceUntilIdle()
                
                // 1. 初期状態: 未認証
                viewModel.uiState.value.isAuthenticated shouldBe false
                
                // 2. 認証開始
                viewModel.startAuth()
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.uiState.value.isAuthenticating shouldBe true
                
                // 3. 認証コールバック処理
                viewModel.handleAuthCallback("valid_code")
                testDispatcher.scheduler.advanceUntilIdle()
                
                // 4. 最終状態: 認証完了
                viewModel.uiState.value.isAuthenticated shouldBe true
                viewModel.uiState.value.isAuthenticating shouldBe false
                viewModel.uiState.value.error shouldBe null
                
                // 全ての依存関係が正しく呼ばれたことを確認
                coVerify { authUseCase.getAuthUrl() }
                coVerify { authUseCase.handleAuthCallback("valid_code") }
                
                println("✅ 統合テスト: 実際のフローを検証")
            }
        }

        context("4. 実装の詳細テスト - エラー処理") {
            it("ViewModelの内部エラー処理ロジックをテスト") {
                val authUseCase = mockk<AnnictAuthUseCase>()
                val context = mockk<Context>()
                val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
                
                // エラーが発生するシナリオを設定
                coEvery { authUseCase.isAuthenticated() } throws RuntimeException("ネットワークエラー")
                
                val viewModel = MainViewModel(
                    authUseCase, 
                    customTabsIntentFactory, 
                    context
                )
                
                testDispatcher.scheduler.advanceUntilIdle()
                
                // エラー処理の実装をテスト
                viewModel.uiState.value.error shouldNotBe null
                viewModel.uiState.value.error shouldBe "ネットワークエラー"
                viewModel.uiState.value.isLoading shouldBe false
                
                println("✅ エラー処理テスト: 実装の詳細な動作を検証")
            }
        }

        context("5. パフォーマンステスト - 非同期処理") {
            it("ViewModelの非同期処理タイミングをテスト") {
                val authUseCase = mockk<AnnictAuthUseCase>()
                val context = mockk<Context>()
                val customTabsIntent = mockk<CustomTabsIntent>(relaxUnitFun = true)
                val customTabsIntentFactory = mockk<CustomTabsIntentFactory>()
                
                every { customTabsIntentFactory.create() } returns customTabsIntent
                every { customTabsIntent.launchUrl(any(), any()) } just Runs
                coEvery { authUseCase.isAuthenticated() } returns false
                coEvery { authUseCase.getAuthUrl() } returns "https://example.com/auth"
                
                val viewModel = MainViewModel(
                    authUseCase, 
                    customTabsIntentFactory, 
                    context
                )
                
                testDispatcher.scheduler.advanceUntilIdle()
                
                // 認証開始時の状態変化をテスト
                viewModel.startAuth()
                
                // まだ非同期処理が完了していない
                viewModel.uiState.value.isAuthenticating shouldBe true
                
                // 非同期処理を完了させる
                testDispatcher.scheduler.advanceUntilIdle()
                
                // 認証開始処理が完了
                coVerify { authUseCase.getAuthUrl() }
                
                println("✅ 非同期処理テスト: タイミングを詳細に検証")
            }
        }
    }

    describe("テストアプローチの使い分け") {
        it("各アプローチの適用場面") {
            println("""
            === ViewModelテストアプローチの使い分け ===
            
            1. インターフェースベースのテスト
               - 用途: UIコンポーネントのテスト
               - 特徴: 実装に依存しない、高速、状態操作が簡単
               - 例: ComposeのUI状態テスト、画面遷移テスト
            
            2. 実装テスト  
               - 用途: ビジネスロジックのテスト
               - 特徴: 実際の実装を検証、依存関係のモック必要
               - 例: 認証ロジック、データ変換、エラー処理
            
            3. 統合テスト
               - 用途: エンドツーエンドのフロー検証
               - 特徴: 実際の依存関係を使用、現実的なシナリオ
               - 例: 認証フロー全体、複雑な状態遷移
               
            4. テスト専用インターフェース (TestableViewModel)
               - 用途: 複雑な状態のセットアップ
               - 特徴: テスト時のみ使用、本番コードからは隠蔽
               - 例: エラー状態の直接設定、特定の状態からの開始
            """.trimIndent())
        }
    }
})