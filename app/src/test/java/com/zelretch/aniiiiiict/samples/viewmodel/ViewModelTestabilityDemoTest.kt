package com.zelretch.aniiiiiict.samples.viewmodel

import com.zelretch.aniiiiiict.MainViewModel
import com.zelretch.aniiiiiict.domain.usecase.AnnictAuthUseCase
import com.zelretch.aniiiiiict.testing.MainUiStateBuilder
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import com.zelretch.aniiiiiict.ui.base.CustomTabsIntentFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * ViewModelテスト容易性向上のデモンストレーション
 * インターフェースベースのテストとテスト用ユーティリティの活用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTestabilityDemoTest : BehaviorSpec({

    lateinit var testDispatcher: TestDispatcher

    beforeTest {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("改善されたViewModel testability") {

        `when`("インターフェースベースのテスト") {
            then("ViewModelContractを通じてUI状態にアクセスできる") {
                // Mock dependencies
                val mockAuthUseCase = mockk<AnnictAuthUseCase>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<CustomTabsIntentFactory>(relaxed = true)

                coEvery { mockAuthUseCase.isAuthenticated() } returns false

                // ViewModelを作成
                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockContext
                )
                // initブロックの処理を待機
                testDispatcher.scheduler.runCurrent()

                // インターフェースとして参照
                val viewModelContract: MainViewModelContract = viewModel

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
                val mockAuthUseCase = mockk<AnnictAuthUseCase>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<CustomTabsIntentFactory>(relaxed = true)

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockContext
                )
                // initブロックの処理を待機
                testDispatcher.scheduler.runCurrent()

                val viewModelContract: MainViewModelContract = viewModel

                // 複雑な状態をワンステップで設定
                viewModel.internalUiState.value = MainUiStateBuilder.custom(
                    isLoading = false,
                    error = "テストエラー",
                    isAuthenticating = true,
                    isAuthenticated = false
                )

                // 設定した状態が反映されることを確認
                with(viewModelContract.uiState.value) {
                    error shouldBe "テストエラー"
                    isAuthenticating shouldBe true
                    isAuthenticated shouldBe false
                }

                // エラーを設定
                viewModel.internalUiState.value = viewModel.uiState.value.copy(error = "新しいエラー")
                viewModelContract.uiState.value.error shouldBe "新しいエラー"

                // ローディング状態を設定
                viewModel.internalUiState.value = viewModel.uiState.value.copy(isLoading = true)
                viewModelContract.uiState.value.isLoading shouldBe true

                // 状態をリセット
                viewModel.internalUiState.value = MainUiStateBuilder.custom()
                with(viewModelContract.uiState.value) {
                    isLoading shouldBe false
                    error shouldBe null
                }
            }
        }

        `when`("既存のテスト手法との比較") {
            then("改善後: 状態を直接設定してテストを簡素化") {
                val mockAuthUseCase = mockk<AnnictAuthUseCase>(relaxed = true)
                val mockContext = mockk<android.content.Context>(relaxed = true)
                val mockCustomTabsIntentFactory = mockk<CustomTabsIntentFactory>(relaxed = true)

                val viewModel = MainViewModel(
                    mockAuthUseCase,
                    mockCustomTabsIntentFactory,
                    mockContext
                )
                // initブロックの処理を待機
                testDispatcher.scheduler.runCurrent()

                val viewModelContract: MainViewModelContract = viewModel

                // エラー状態を直接設定してテストを開始
                viewModel.internalUiState.value = MainUiStateBuilder.error("認証エラー")

                // エラー状態からの回復をテスト
                viewModelContract.clearError()
                viewModelContract.uiState.value.error shouldBe null

                // 認証済み状態を直接設定
                viewModel.internalUiState.value = MainUiStateBuilder.authenticated()

                viewModelContract.uiState.value.isAuthenticated shouldBe true
            }
        }
    }
})
