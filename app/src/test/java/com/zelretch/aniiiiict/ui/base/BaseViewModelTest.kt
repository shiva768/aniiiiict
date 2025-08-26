package com.zelretch.aniiiiict.ui.base

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * BaseViewModelの単体テスト
 *
 * 複雑なローディング処理とエラーハンドリング、最小限のローディング時間の確保など
 * BaseViewModelが提供する共通機能の品質を保証する
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest : BehaviorSpec({

    lateinit var testDispatcher: TestDispatcher

    beforeTest {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("BaseViewModelの実装") {

        lateinit var viewModel: TestableBaseViewModel

        beforeEach {
            viewModel = TestableBaseViewModel()
        }

        `when`("executeWithLoadingが正常に完了する場合") {
            then("ローディング状態が適切に管理される") {
                runTest(testDispatcher) {
                    var processingStarted = false
                    var processingCompleted = false

                    // 実行前の初期状態確認
                    viewModel.loadingState shouldBe false
                    viewModel.errorState shouldBe null

                    viewModel.executeTestWithLoading {
                        processingStarted = true
                        delay(500) // 短い処理をシミュレート
                        processingCompleted = true
                    }
                    testDispatcher.scheduler.runCurrent()

                    // 処理開始時にローディングが開始される
                    viewModel.loadingState shouldBe true
                    viewModel.errorState shouldBe null
                    processingStarted shouldBe true

                    // 時間を進めて最小ローディング時間まで完了させる
                    testDispatcher.scheduler.advanceTimeBy(1001)

                    // 処理完了後
                    processingCompleted shouldBe true
                    viewModel.loadingState shouldBe false
                    viewModel.errorState shouldBe null
                }
            }
        }

        `when`("executeWithLoadingで例外が発生する場合") {
            then("エラー状態が適切に設定される") {
                runTest(testDispatcher) {
                    val errorMessage = "テスト用エラー"
                    val exception = RuntimeException(errorMessage)

                    viewModel.executeTestWithLoading {
                        throw exception
                    }
                    testDispatcher.scheduler.runCurrent()

                    // 完了を待つ
                    testDispatcher.scheduler.advanceTimeBy(1001)

                    // エラー処理の確認 - ErrorHandlerがユーザー向けメッセージを返す
                    viewModel.loadingState shouldBe false
                    viewModel.errorState shouldBe "処理中にエラーが発生しました"
                }
            }
        }

        `when`("executeWithLoadingで例外メッセージがnullの場合") {
            then("デフォルトエラーメッセージが設定される") {
                runTest(testDispatcher) {
                    val exception = RuntimeException(null as String?)

                    viewModel.executeTestWithLoading {
                        throw exception
                    }
                    testDispatcher.scheduler.runCurrent()

                    // 完了を待つ
                    testDispatcher.scheduler.advanceTimeBy(1001)

                    viewModel.errorState shouldBe "処理中にエラーが発生しました"
                }
            }
        }

        `when`("処理時間が1秒未満の場合") {
            then("最小限のローディング時間（1秒）が確保される") {
                runTest(testDispatcher) {
                    val startTime = testDispatcher.scheduler.currentTime
                    var processingCompleted = false

                    viewModel.executeTestWithLoading {
                        delay(300) // 300ミリ秒の短い処理
                        processingCompleted = true
                    }
                    testDispatcher.scheduler.runCurrent()

                    // 処理は早く完了するが、ローディングは継続
                    testDispatcher.scheduler.advanceTimeBy(301)
                    processingCompleted shouldBe true
                    viewModel.loadingState shouldBe true // まだローディング中

                    // 最小時間まで進める
                    testDispatcher.scheduler.advanceTimeBy(700) // 合計1001ms

                    // 最小時間後にローディング終了
                    viewModel.loadingState shouldBe false
                    val totalTime = testDispatcher.scheduler.currentTime - startTime
                    totalTime shouldBe 1001L
                }
            }
        }

        `when`("処理時間が1秒以上の場合") {
            then("追加の待機時間は発生しない") {
                runTest(testDispatcher) {
                    val startTime = testDispatcher.scheduler.currentTime

                    viewModel.executeTestWithLoading {
                        delay(1500) // 1.5秒の長い処理
                    }
                    testDispatcher.scheduler.runCurrent()

                    testDispatcher.scheduler.advanceTimeBy(1501)

                    viewModel.loadingState shouldBe false
                    val totalTime = testDispatcher.scheduler.currentTime - startTime
                    totalTime shouldBe 1501L // 余分な待機時間なし
                }
            }
        }

        `when`("複数の処理が並行して実行される場合") {
            then("それぞれが独立してローディング状態を管理する") {
                runTest(testDispatcher) {
                    var process1Completed = false
                    var process2Completed = false

                    // 2つの処理を並行実行
                    viewModel.executeTestWithLoading {
                        delay(200)
                        process1Completed = true
                    }

                    viewModel.executeTestWithLoading {
                        delay(800)
                        process2Completed = true
                    }
                    testDispatcher.scheduler.runCurrent()

                    // 両方の処理でローディングが開始される
                    viewModel.loadingState shouldBe true

                    // 時間を進めて確認
                    testDispatcher.scheduler.advanceTimeBy(1001)

                    process1Completed shouldBe true
                    process2Completed shouldBe true
                    viewModel.loadingState shouldBe false
                }
            }
        }

        `when`("エラー状態をクリアする") {
            then("エラー状態がnullになる") {
                runTest(testDispatcher) {
                    // 先にエラー状態を設定
                    viewModel.updateErrorState("テストエラー")
                    viewModel.errorState shouldNotBe null

                    // エラーをクリア
                    viewModel.updateErrorState(null)
                    viewModel.errorState shouldBe null
                }
            }
        }

        `when`("ローディング状態を手動で更新する") {
            then("指定した状態が反映される") {
                runTest(testDispatcher) {
                    viewModel.loadingState shouldBe false

                    viewModel.updateLoadingState(true)
                    viewModel.loadingState shouldBe true

                    viewModel.updateLoadingState(false)
                    viewModel.loadingState shouldBe false
                }
            }
        }
    }
})

/**
 * テスト用のBaseViewModel実装
 * 抽象メソッドを実装してテスト可能にする
 */
private class TestableBaseViewModel : BaseViewModel() {
    data class TestUiState(
        override val isLoading: Boolean = false,
        override val error: String? = null
    ) : BaseUiState(isLoading, error)

    private val uiState = MutableStateFlow(TestUiState())

    // テスト用のアクセサ
    val loadingState: Boolean get() = uiState.value.isLoading
    val errorState: String? get() = uiState.value.error

    override fun updateLoadingState(isLoading: Boolean) {
        uiState.value = uiState.value.copy(isLoading = isLoading)
    }

    override fun updateErrorState(error: String?) {
        uiState.value = uiState.value.copy(error = error)
    }

    // テスト用のpublicメソッド
    fun executeTestWithLoading(block: suspend () -> Unit) {
        executeWithLoading(block)
    }
}
