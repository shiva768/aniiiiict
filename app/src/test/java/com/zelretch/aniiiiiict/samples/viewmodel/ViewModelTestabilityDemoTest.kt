package com.zelretch.aniiiiiict.samples.viewmodel

import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.testing.MainUiStateBuilder
import com.zelretch.aniiiiiict.testing.TestableMainViewModel
import com.zelretch.aniiiiiict.testing.asTestable
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

/**
 * ViewModelテスト容易性向上のデモンストレーション
 * インターフェースベースのテストとテスト用ユーティリティの活用例
 */
class ViewModelTestabilityDemoTest : BehaviorSpec({

    given("改善されたViewModel testability") {

        `when`("インターフェースベースのテスト") {
            then("ViewModelContractを通じてUI状態にアクセスできる") {
                // Mock dependencies
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory =
                    mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)

                coEvery { mockAuthUseCase.isAuthenticated() } returns false

                // ViewModelを作成
                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )

                // インターフェースとして参照
                val viewModelContract: MainViewModelContract = viewModel
                val testableViewModel: TestableMainViewModel = viewModel.asTestable()

                // インターフェース経由でのアクセス
                val initialState = viewModelContract.uiState.value
                initialState.isAuthenticated shouldBe false
                initialState.error shouldBe null

                // インターフェース経由でのメソッド呼び出し
                viewModelContract.clearError()

                // 状態変更が反映されることを確認
                viewModelContract.uiState.value.error shouldBe null
            }
        }

        `when`("TestableViewModelによる状態操作") {
            then("UI状態を直接設定できる") {
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory =
                    mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )

                val testableViewModel: TestableMainViewModel = viewModel.asTestable()
                val viewModelContract: MainViewModelContract = viewModel

                // 複雑な状態をワンステップで設定
                testableViewModel.setUiStateForTest(
                    MainUiStateBuilder.custom(
                        isLoading = false,
                        error = "テストエラー",
                        isAuthenticating = true,
                        isAuthenticated = false
                    )
                )

                // 設定した状態が反映されることを確認
                with(viewModelContract.uiState.value) {
                    error shouldBe "テストエラー"
                    isAuthenticating shouldBe true
                    isAuthenticated shouldBe false
                }

                // ユーティリティメソッドでエラーを設定
                testableViewModel.setErrorForTest("新しいエラー")
                viewModelContract.uiState.value.error shouldBe "新しいエラー"

                // ローディング状態を設定
                testableViewModel.setLoadingForTest(true)
                viewModelContract.uiState.value.isLoading shouldBe true

                // 状態をリセット
                testableViewModel.resetToInitialState()
                with(viewModelContract.uiState.value) {
                    isLoading shouldBe false
                    error shouldBe null
                }
            }
        }

        `when`("既存のテスト手法との比較") {
            then("従来: 複雑なセットアップが必要") {
                // 従来の方法では、特定の状態に到達するために
                // 複数のメソッド呼び出しとモックの設定が必要だった
                // 例: エラー状態をテストするために実際にエラーを発生させる必要があった
            }

            then("改善後: 状態を直接設定してテストを簡素化") {
                val mockAuthUseCase = mockk<com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase>(relaxed = true)
                val mockLogger = mockk<com.zelretch.aniiiiiict.util.Logger>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory =
                    mockk<com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory>(relaxed = true)

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockLogger,
                    mockContext
                )

                val testableViewModel: TestableMainViewModel = viewModel.asTestable()
                val viewModelContract: MainViewModelContract = viewModel

                // エラー状態を直接設定してテストを開始
                testableViewModel.setErrorForTest("認証エラー")

                // エラー状態からの回復をテスト
                viewModelContract.clearError()
                viewModelContract.uiState.value.error shouldBe null

                // 認証済み状態を直接設定
                testableViewModel.setUiStateForTest(
                    MainUiStateBuilder.authenticated()
                )

                viewModelContract.uiState.value.isAuthenticated shouldBe true
            }
        }
    }
})
