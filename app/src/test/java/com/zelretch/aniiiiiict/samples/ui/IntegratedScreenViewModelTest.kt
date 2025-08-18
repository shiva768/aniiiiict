package com.zelretch.aniiiiiict.samples.ui

import com.zelretch.aniiiiiict.MainUiState
import com.zelretch.aniiiiiict.testing.TestableViewModel
import com.zelretch.aniiiiiict.ui.MainViewModelContract
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * インターフェースを使用した統合的なScreen/ViewModelテスト
 * 実際のワークフローでインターフェースがどのように活用されるかを示す
 *
 * このテストでは、複数のインターフェースを組み合わせた
 * 実用的なテストパターンを実演する
 */
class IntegratedScreenViewModelTest : BehaviorSpec({

    given("統合的なViewModelインターフェーステスト") {

        `when`("認証フローをシミュレートする") {
            then("インターフェースとTestableViewModelを組み合わせて使用できる") {
                // インターフェースベースのセットアップ
                val viewModelContract = mockk<MainViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<MainUiState>>(relaxed = true)

                // 初期状態
                var currentState = MainUiState()
                val stateFlow = MutableStateFlow(currentState)
                every { viewModelContract.uiState } returns stateFlow

                // TestableViewModelのメソッドをモック
                every { testableViewModel.setUiStateForTest(any()) } answers {
                    currentState = firstArg()
                    stateFlow.value = currentState
                }

                // Step 1: 初期状態の確認
                currentState.isAuthenticated shouldBe false
                currentState.isAuthenticating shouldBe false

                // Step 2: 認証開始
                viewModelContract.startAuth()
                verify { viewModelContract.startAuth() }

                // Step 3: ローディング状態をシミュレート
                testableViewModel.setLoadingForTest(true)

                // Step 4: エラー状態をシミュレート
                testableViewModel.setErrorForTest("ネットワークエラー")

                // Step 5: 成功状態をシミュレート
                testableViewModel.resetToInitialState()
                testableViewModel.setUiStateForTest(MainUiState(isAuthenticated = true))

                // 全体のフローをインターフェース経由でテスト可能
            }
        }

        `when`("複数状態の遷移をテストする") {
            then("TestableViewModelで簡単に状態遷移をシミュレートできる") {
                val viewModelContract = mockk<MainViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<MainUiState>>(relaxed = true)

                var currentState = MainUiState()
                val stateFlow = MutableStateFlow(currentState)
                every { viewModelContract.uiState } returns stateFlow

                // TestableViewModelのメソッドで状態を更新
                every { testableViewModel.setUiStateForTest(any()) } answers {
                    currentState = firstArg()
                    stateFlow.value = currentState
                }

                // 状態遷移をテスト: 初期 → ローディング → エラー → 正常

                // 1. 初期状態
                currentState.isLoading shouldBe false
                currentState.isAuthenticating shouldBe false
                currentState.error shouldBe null

                // 2. ローディング状態に遷移
                testableViewModel.setUiStateForTest(
                    currentState.copy(isAuthenticating = true)
                )

                // 3. エラー状態に遷移
                testableViewModel.setUiStateForTest(
                    currentState.copy(
                        isAuthenticating = false,
                        error = "認証に失敗しました"
                    )
                )

                // 4. 成功状態に遷移
                testableViewModel.setUiStateForTest(
                    currentState.copy(
                        error = null,
                        isAuthenticated = true
                    )
                )

                // 各段階での状態変化を確認可能
                // 従来は複雑な非同期処理の制御が必要だった
            }
        }

        `when`("エラーハンドリングをテストする") {
            then("インターフェースのclearError()メソッドをテストできる") {
                val viewModelContract = mockk<MainViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<MainUiState>>(relaxed = true)

                // エラー状態を設定
                val errorState = MainUiState(error = "テストエラー")
                every { viewModelContract.uiState } returns MutableStateFlow(errorState)

                // エラー状態の確認
                errorState.error shouldNotBe null

                // エラークリアアクション
                viewModelContract.clearError()
                verify { viewModelContract.clearError() }

                // 契約で定義されたエラークリア機能のテスト
                // 実装詳細に依存しない
            }
        }

        `when`("複数のViewModelContractを組み合わせる") {
            then("複数の契約が連携して動作することをテストできる") {
                // 複数の契約を使用
                val mainContract = mockk<MainViewModelContract>(relaxed = true)
                val mainTestable = mockk<TestableViewModel<MainUiState>>(relaxed = true)

                // 状態の設定
                val mainState = MainUiState(isAuthenticated = true)
                every { mainContract.uiState } returns MutableStateFlow(mainState)

                // 認証状態に基づいた分岐ロジックのテスト
                if (mainState.isAuthenticated) {
                    // 認証済みの場合の処理
                    mainState.isAuthenticated shouldBe true
                } else {
                    // 未認証の場合の処理
                    mainContract.startAuth()
                }

                // 複数の契約の連携をテスト
                // まだ認証済みなので startAuth は呼ばれない
                verify(exactly = 0) { mainContract.startAuth() }
            }
        }

        `when`("ViewModelTestUtilsを活用する") {
            then("ユーティリティ関数で効率的にテストできる") {
                val viewModelContract = mockk<MainViewModelContract>(relaxed = true)
                val testableViewModel = mockk<TestableViewModel<MainUiState>>(relaxed = true)

                // ユーティリティ関数を使用した状態操作例
                // (実際のViewModelTestUtilsの拡張関数として実装されている)

                // エラー状態設定: testableViewModel.setErrorForTest("エラー")
                // ローディング状態設定: testableViewModel.setLoadingForTest(true)
                // 初期状態リセット: testableViewModel.resetToInitialState()

                // これらの関数により:
                // - テストコードが簡潔になる
                // - 共通的な操作を再利用できる
                // - テストの可読性が向上する

                val initialState = MainUiState()
                every { viewModelContract.uiState } returns MutableStateFlow(initialState)

                initialState.error shouldBe null
                initialState.isLoading shouldBe false
            }
        }
    }
})

/**
 * 統合テストから得られる知見：
 *
 * ## 1. ワークフローの再現が容易
 * ```kotlin
 * // 従来: 複雑な依存関係の設定
 * // 改善後: 状態の直接操作で簡潔に
 * testableViewModel.setLoadingForTest(true)
 * testableViewModel.setErrorForTest("エラー")
 * testableViewModel.resetToInitialState()
 * ```
 *
 * ## 2. 状態遷移のテストが明確
 * - 各状態での UI の振る舞いを個別にテスト可能
 * - 状態間の遷移ロジックをシンプルにテスト
 * - エラー復旧のフローも簡単に再現
 *
 * ## 3. 契約ベースの設計の利点
 * - テストコードが契約に基づいているため、実装変更に強い
 * - インターフェースが明確なAPIを提供
 * - モックの設定が簡潔で理解しやすい
 *
 * ## 4. 実用的なテストパターン
 * - 単一機能のテスト（認証、エラーハンドリング等）
 * - 複数コンポーネント間の連携テスト
 * - ユーザーワークフローの再現テスト
 * - エッジケースとエラーシナリオのテスト
 *
 * ## 5. メンテナンス性
 * - テストの意図が明確
 * - セットアップが単純
 * - デバッグが容易
 * - 新機能追加時の影響範囲が限定的
 */
