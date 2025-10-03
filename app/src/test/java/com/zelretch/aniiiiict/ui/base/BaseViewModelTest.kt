package com.zelretch.aniiiiict.ui.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BaseViewModelの単体テスト
 *
 * 複雑なローディング処理とエラーハンドリング、最小限のローディング時間の確保など
 * BaseViewModelが提供する共通機能の品質を保証する
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("BaseViewModel")
class BaseViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: TestableBaseViewModel

    @BeforeEach
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        viewModel = TestableBaseViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("executeWithLoading正常完了")
    inner class NormalCompletion {

        @Test
        @DisplayName("ローディング状態が適切に管理される")
        fun ローディング状態が適切に管理される() = runTest(testDispatcher) {
            // Given
            var processingStarted = false
            var processingCompleted = false

            // When: 実行前の初期状態確認
            assertFalse(viewModel.loadingState)
            assertNull(viewModel.errorState)

            viewModel.executeTestWithLoading {
                processingStarted = true
                delay(500)
                processingCompleted = true
            }
            testDispatcher.scheduler.runCurrent()

            // Then: 処理開始時にローディングが開始される
            assertTrue(viewModel.loadingState)
            assertNull(viewModel.errorState)
            assertTrue(processingStarted)

            // When: 時間を進めて最小ローディング時間まで完了させる
            testDispatcher.scheduler.advanceTimeBy(1001)

            // Then: 処理完了後
            assertTrue(processingCompleted)
            assertFalse(viewModel.loadingState)
            assertNull(viewModel.errorState)
        }
    }

    @Nested
    @DisplayName("executeWithLoading例外発生")
    inner class ExceptionHandling {

        @Test
        @DisplayName("エラー状態が適切に設定される")
        fun エラー状態が適切に設定される() = runTest(testDispatcher) {
            // Given
            val errorMessage = "テスト用エラー"
            val exception = RuntimeException(errorMessage)

            // When
            viewModel.executeTestWithLoading {
                throw exception
            }
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceTimeBy(1001)

            // Then: ErrorHandlerがユーザー向けメッセージを返す
            assertFalse(viewModel.loadingState)
            assertEquals("処理中にエラーが発生しました", viewModel.errorState)
        }

        @Test
        @DisplayName("例外メッセージがnullの場合デフォルトエラーメッセージが設定される")
        fun 例外メッセージがnullの場合デフォルトエラーメッセージが設定される() = runTest(testDispatcher) {
            // Given
            val exception = RuntimeException(null as String?)

            // When
            viewModel.executeTestWithLoading {
                throw exception
            }
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceTimeBy(1001)

            // Then
            assertEquals("処理中にエラーが発生しました", viewModel.errorState)
        }
    }

    @Nested
    @DisplayName("最小ローディング時間")
    inner class MinimumLoadingTime {

        @Test
        @DisplayName("処理時間が1秒未満の場合最小限のローディング時間が確保される")
        fun 処理時間が1秒未満の場合最小限のローディング時間が確保される() = runTest(testDispatcher) {
            // Given
            val startTime = testDispatcher.scheduler.currentTime
            var processingCompleted = false

            // When
            viewModel.executeTestWithLoading {
                delay(300)
                processingCompleted = true
            }
            testDispatcher.scheduler.runCurrent()

            // Then: 処理は早く完了するが、ローディングは継続
            testDispatcher.scheduler.advanceTimeBy(301)
            assertTrue(processingCompleted)
            assertTrue(viewModel.loadingState) // まだローディング中

            // When: 最小時間まで進める
            testDispatcher.scheduler.advanceTimeBy(700) // 合計1001ms

            // Then: 最小時間後にローディング終了
            assertFalse(viewModel.loadingState)
            val totalTime = testDispatcher.scheduler.currentTime - startTime
            assertEquals(1001L, totalTime)
        }

        @Test
        @DisplayName("処理時間が1秒以上の場合追加の待機時間は発生しない")
        fun 処理時間が1秒以上の場合追加の待機時間は発生しない() = runTest(testDispatcher) {
            // Given
            val startTime = testDispatcher.scheduler.currentTime

            // When
            viewModel.executeTestWithLoading {
                delay(1500)
            }
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceTimeBy(1501)

            // Then
            assertFalse(viewModel.loadingState)
            val totalTime = testDispatcher.scheduler.currentTime - startTime
            assertEquals(1501L, totalTime) // 余分な待機時間なし
        }
    }

    @Nested
    @DisplayName("並行処理")
    inner class ConcurrentProcessing {

        @Test
        @DisplayName("複数の処理が並行して実行される場合それぞれが独立してローディング状態を管理する")
        fun 複数の処理が並行して実行される場合それぞれが独立してローディング状態を管理する() = runTest(testDispatcher) {
            // Given
            var process1Completed = false
            var process2Completed = false

            // When: 2つの処理を並行実行
            viewModel.executeTestWithLoading {
                delay(200)
                process1Completed = true
            }

            viewModel.executeTestWithLoading {
                delay(800)
                process2Completed = true
            }
            testDispatcher.scheduler.runCurrent()

            // Then: 両方の処理でローディングが開始される
            assertTrue(viewModel.loadingState)

            // When: 時間を進めて確認
            testDispatcher.scheduler.advanceTimeBy(1001)

            // Then
            assertTrue(process1Completed)
            assertTrue(process2Completed)
            assertFalse(viewModel.loadingState)
        }
    }

    @Nested
    @DisplayName("状態管理")
    inner class StateManagement {

        @Test
        @DisplayName("エラー状態をクリアするとnullになる")
        fun エラー状態をクリアするとnullになる() = runTest(testDispatcher) {
            // Given: 先にエラー状態を設定
            viewModel.updateErrorState("テストエラー")
            assertNotNull(viewModel.errorState)

            // When: エラーをクリア
            viewModel.updateErrorState(null)

            // Then
            assertNull(viewModel.errorState)
        }

        @Test
        @DisplayName("ローディング状態を手動で更新すると指定した状態が反映される")
        fun ローディング状態を手動で更新すると指定した状態が反映される() = runTest(testDispatcher) {
            // Given
            assertFalse(viewModel.loadingState)

            // When & Then
            viewModel.updateLoadingState(true)
            assertTrue(viewModel.loadingState)

            viewModel.updateLoadingState(false)
            assertFalse(viewModel.loadingState)
        }
    }
}

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
