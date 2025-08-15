package com.zelretch.aniiiiiict.testing

import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * プロダクションコードを汚染しない新しいテストアプローチのデモンストレーション
 * 
 * このテストは以下の問題を解決します：
 * 1. プロダクションコードにテスト専用メソッドが含まれる問題
 * 2. テスト用インターフェースの実装がプロダクションに漏れる問題
 * 3. プロダクションビルドにテストコードが含まれる問題
 */
class CleanTestingApproachDemoTest : BehaviorSpec({

    given("プロダクションコードを汚染しないテストアプローチ") {
        
        `when`("従来の問題があったアプローチ") {
            then("プロダクションコードにTestableViewModelの実装があった") {
                // ❌ 以前の問題：
                // @HiltViewModel
                // class MainViewModel : MainViewModelContract, TestableViewModel<MainUiState> {
                //     // プロダクションコードにテスト用メソッド
                //     override fun setUiStateForTest(state: MainUiState) { ... }
                //     override fun setErrorForTest(error: String?) { ... }
                //     override fun setLoadingForTest(isLoading: Boolean) { ... }
                // }
                
                // この方法は以下の問題があった：
                // 1. プロダクションコードが肥大化
                // 2. テスト専用メソッドが本番環境に含まれる
                // 3. 誤ってプロダクションでテストメソッドが呼ばれる可能性
            }
        }
        
        `when`("新しい改善されたアプローチ") {
            then("プロダクションコードは純粋でテスト機能は分離されている") {
                // Mock dependencies
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)
                
                coEvery { mockAuthUseCase.isAuthenticated() } returns false
                
                // ✅ 新しいアプローチ：
                // プロダクションコードは純粋
                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )
                
                // プロダクション用インターフェース
                val contract: MainViewModelContract = viewModel
                
                // テスト専用機能はテストソースセットでのみ提供
                val testable: TestableMainViewModel = viewModel.asTestable()
                
                // プロダクションインターフェースは正常に動作
                contract.uiState.value.isAuthenticated shouldBe false
                contract.clearError()
                
                // テスト機能はテストでのみ利用可能
                testable.setUiStateForTest(MainUiStateBuilder.authenticated())
                contract.uiState.value.isAuthenticated shouldBe true
                
                testable.setErrorForTest("テストエラー")
                contract.uiState.value.error shouldBe "テストエラー"
                
                testable.resetToInitialState()
                contract.uiState.value.error shouldBe null
                contract.uiState.value.isAuthenticated shouldBe false
            }
        }
        
        `when`("メリットの比較") {
            then("プロダクションコードの純度が保たれる") {
                // ✅ プロダクションコードには一切のテスト専用コードが含まれない
                // ✅ ViewModelクラスはビジネスロジックにのみ集中できる
                // ✅ プロダクションビルドサイズの削減
                // ✅ 誤用によるバグリスクの排除
            }
            
            then("テスト容易性は維持される") {
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)
                
                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )
                
                val testable = viewModel.asTestable()
                
                // ✅ 状態操作はテストでは同様に簡単
                testable.setErrorForTest("ネットワークエラー")
                testable.setLoadingForTest(true)
                testable.setUiStateForTest(
                    MainUiStateBuilder.custom(
                        isLoading = false,
                        error = null,
                        isAuthenticating = true
                    )
                )
                
                // ✅ テストの表現力は変わらず
                viewModel.uiState.value.isAuthenticating shouldBe true
            }
            
            then("実装テストも変わらずサポートされる") {
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)
                
                coEvery { mockAuthUseCase.getAuthUrl() } returns "http://example.com/auth"
                
                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )
                
                // ✅ 実装テストは引き続き実際のメソッドをテスト
                viewModel.startAuth()
                
                // ビジネスロジックの検証
                coVerify { mockAuthUseCase.getAuthUrl() }
            }
        }
    }
    
    given("複数のViewModelでの汎用的な使用") {
        
        `when`("異なるViewModelで同じアプローチを使用") {
            then("パターンが一貫している") {
                // このアプローチは他のViewModelでも同様に使用可能
                // 例：TrackViewModel, HistoryViewModel, etc.
                
                // val trackViewModel = TrackViewModel(...)
                // val testableTrack = trackViewModel.asTestable()
                // testableTrack.setUiStateForTest(TrackUiStateBuilder.withPrograms(...))
                
                // 各ViewModelに対してas Testable()拡張を提供することで
                // 統一的なテストアプローチが可能
            }
        }
    }
})