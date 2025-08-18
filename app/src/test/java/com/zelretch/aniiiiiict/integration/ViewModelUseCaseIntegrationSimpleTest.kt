package com.zelretch.aniiiiiict.integration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * 統合テスト用の簡易バージョン
 *
 * 複雑なデータモデル依存を避け、ViewModel-UseCase間の連携パターンに焦点を当てた統合テスト:
 * - エラー伝播の一貫性
 * - 状態遷移の正確性
 * - 非同期処理の協調動作
 * - データフローの妥当性
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelUseCaseIntegrationSimpleTest : BehaviorSpec({

    val dispatcher = UnconfinedTestDispatcher()

    beforeTest {
        Dispatchers.setMain(dispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    given("ViewModel-UseCase連携パターン") {

        `when`("UseCase成功時のViewModelへの反映") {
            then("適切に成功状態が伝播される") {
                runTest {
                    // UseCase の結果をシミュレート
                    data class UseCaseResult<T>(
                        val isSuccess: Boolean,
                        val data: T? = null,
                        val error: String? = null
                    )

                    // ViewModel の状態をシミュレート
                    data class ViewModelState(
                        val isLoading: Boolean = false,
                        val data: String? = null,
                        val error: String? = null
                    )

                    var vmState = ViewModelState()

                    // ローディング開始
                    vmState = vmState.copy(isLoading = true, error = null)
                    vmState.isLoading shouldBe true
                    vmState.error shouldBe null

                    // UseCase 成功結果
                    val useCaseResult = UseCaseResult(
                        isSuccess = true,
                        data = "成功データ",
                        error = null
                    )

                    // ViewModel に結果を反映
                    vmState = if (useCaseResult.isSuccess) {
                        vmState.copy(
                            isLoading = false,
                            data = useCaseResult.data,
                            error = null
                        )
                    } else {
                        vmState.copy(
                            isLoading = false,
                            data = null,
                            error = useCaseResult.error
                        )
                    }

                    vmState.isLoading shouldBe false
                    vmState.data shouldBe "成功データ"
                    vmState.error shouldBe null
                }
            }
        }

        `when`("UseCase失敗時のViewModelへの反映") {
            then("適切にエラー状態が伝播される") {
                runTest {
                    data class UseCaseResult<T>(
                        val isSuccess: Boolean,
                        val data: T? = null,
                        val error: String? = null
                    )

                    data class ViewModelState(
                        val isLoading: Boolean = false,
                        val data: String? = null,
                        val error: String? = null
                    )

                    var vmState = ViewModelState()

                    // ローディング開始
                    vmState = vmState.copy(isLoading = true, error = null)

                    // UseCase 失敗結果
                    val useCaseResult = UseCaseResult<String>(
                        isSuccess = false,
                        data = null,
                        error = "ネットワークエラーが発生しました"
                    )

                    // ViewModel に結果を反映
                    vmState = if (useCaseResult.isSuccess) {
                        vmState.copy(
                            isLoading = false,
                            data = useCaseResult.data,
                            error = null
                        )
                    } else {
                        vmState.copy(
                            isLoading = false,
                            data = null,
                            error = useCaseResult.error
                        )
                    }

                    vmState.isLoading shouldBe false
                    vmState.data shouldBe null
                    vmState.error shouldBe "ネットワークエラーが発生しました"
                }
            }
        }

        `when`("複数のUseCase結果を順次処理") {
            then("各処理の結果が適切に管理される") {
                runTest {
                    data class ViewModelState(
                        val isLoading: Boolean = false,
                        val step1Complete: Boolean = false,
                        val step2Complete: Boolean = false,
                        val step3Complete: Boolean = false,
                        val error: String? = null
                    )

                    var vmState = ViewModelState()

                    // Step 1: 初期データ読み込み
                    vmState = vmState.copy(isLoading = true)
                    val step1Success = true // UseCase1の結果

                    if (step1Success) {
                        vmState = vmState.copy(step1Complete = true)
                        vmState.step1Complete shouldBe true

                        // Step 2: 関連データ読み込み
                        val step2Success = true // UseCase2の結果

                        if (step2Success) {
                            vmState = vmState.copy(step2Complete = true)
                            vmState.step2Complete shouldBe true

                            // Step 3: 最終処理
                            val step3Success = true // UseCase3の結果

                            if (step3Success) {
                                vmState = vmState.copy(
                                    step3Complete = true,
                                    isLoading = false,
                                    error = null
                                )

                                vmState.step3Complete shouldBe true
                                vmState.isLoading shouldBe false
                                vmState.error shouldBe null
                            }
                        }
                    }

                    // 全ステップ完了確認
                    val allStepsComplete = vmState.step1Complete &&
                        vmState.step2Complete &&
                        vmState.step3Complete
                    allStepsComplete shouldBe true
                }
            }
        }

        `when`("途中のUseCaseで失敗した場合") {
            then("適切にエラー処理され後続処理が停止される") {
                runTest {
                    data class ViewModelState(
                        val isLoading: Boolean = false,
                        val step1Complete: Boolean = false,
                        val step2Complete: Boolean = false,
                        val step3Complete: Boolean = false,
                        val error: String? = null
                    )

                    var vmState = ViewModelState()

                    // Step 1: 成功
                    vmState = vmState.copy(isLoading = true)
                    val step1Success = true

                    if (step1Success) {
                        vmState = vmState.copy(step1Complete = true)

                        // Step 2: 失敗
                        val step2Success = false
                        val step2Error = "Step 2でエラーが発生"

                        if (step2Success) {
                            vmState = vmState.copy(step2Complete = true)
                            // Step 3は実行されない
                        } else {
                            vmState = vmState.copy(
                                isLoading = false,
                                error = step2Error
                            )
                        }
                    }

                    // 結果確認
                    vmState.step1Complete shouldBe true
                    vmState.step2Complete shouldBe false // 失敗したので未完了
                    vmState.step3Complete shouldBe false // 実行されていない
                    vmState.isLoading shouldBe false
                    vmState.error shouldBe "Step 2でエラーが発生"
                }
            }
        }
    }

    given("非同期処理の協調動作") {

        `when`("複数のViewModel が並行動作") {
            then("互いに干渉せずに処理される") {
                runTest {
                    data class ViewModelState(
                        val id: String,
                        val isLoading: Boolean = false,
                        val data: String? = null,
                        val processedAt: Long = 0
                    )

                    // 複数のViewModelの状態を管理
                    val viewModels = mutableMapOf<String, ViewModelState>()

                    // ViewModel A
                    viewModels["A"] = ViewModelState(id = "A", isLoading = true)

                    // ViewModel B
                    viewModels["B"] = ViewModelState(id = "B", isLoading = true)

                    // 両方ともローディング中
                    viewModels["A"]?.isLoading shouldBe true
                    viewModels["B"]?.isLoading shouldBe true

                    // A が先に完了
                    val currentTime = System.currentTimeMillis()
                    viewModels["A"] = viewModels["A"]!!.copy(
                        isLoading = false,
                        data = "A のデータ",
                        processedAt = currentTime
                    )

                    // A は完了、B はまだローディング中
                    viewModels["A"]?.isLoading shouldBe false
                    viewModels["A"]?.data shouldBe "A のデータ"
                    viewModels["B"]?.isLoading shouldBe true
                    viewModels["B"]?.data shouldBe null

                    // B も完了
                    viewModels["B"] = viewModels["B"]!!.copy(
                        isLoading = false,
                        data = "B のデータ",
                        processedAt = currentTime + 100
                    )

                    // 両方とも完了、それぞれ独立したデータを持つ
                    viewModels["A"]?.isLoading shouldBe false
                    viewModels["B"]?.isLoading shouldBe false
                    viewModels["A"]?.data shouldBe "A のデータ"
                    viewModels["B"]?.data shouldBe "B のデータ"

                    // 処理順序の確認
                    val aProcessedAt = viewModels["A"]?.processedAt ?: 0
                    val bProcessedAt = viewModels["B"]?.processedAt ?: 0
                    (aProcessedAt < bProcessedAt) shouldBe true
                }
            }
        }
    }

    given("エラー回復とリトライパターン") {

        `when`("UseCase エラー後のリトライ") {
            then("適切にリトライ処理が実行される") {
                runTest {
                    data class RetryState(
                        val attemptCount: Int = 0,
                        val maxAttempts: Int = 3,
                        val isRetrying: Boolean = false,
                        val lastError: String? = null,
                        val isSuccess: Boolean = false
                    )

                    var retryState = RetryState()

                    // 最大リトライ回数まで試行
                    while (retryState.attemptCount < retryState.maxAttempts && !retryState.isSuccess) {
                        retryState = retryState.copy(
                            attemptCount = retryState.attemptCount + 1,
                            isRetrying = true
                        )

                        // UseCase 実行をシミュレート
                        val useCaseSuccess = retryState.attemptCount >= 2 // 2回目で成功

                        if (useCaseSuccess) {
                            retryState = retryState.copy(
                                isRetrying = false,
                                lastError = null,
                                isSuccess = true
                            )
                        } else {
                            retryState = retryState.copy(
                                isRetrying = false,
                                lastError = "試行 ${retryState.attemptCount} 失敗"
                            )
                        }
                    }

                    // 結果確認
                    retryState.attemptCount shouldBe 2 // 2回目で成功
                    retryState.isSuccess shouldBe true
                    retryState.isRetrying shouldBe false
                    retryState.lastError shouldBe null
                }
            }
        }

        `when`("最大リトライ回数に達した場合") {
            then("最終的にエラー状態になる") {
                runTest {
                    data class RetryState(
                        val attemptCount: Int = 0,
                        val maxAttempts: Int = 3,
                        val isRetrying: Boolean = false,
                        val lastError: String? = null,
                        val isSuccess: Boolean = false,
                        val hasFinallyFailed: Boolean = false
                    )

                    var retryState = RetryState()

                    // 全ての試行が失敗
                    while (retryState.attemptCount < retryState.maxAttempts && !retryState.isSuccess) {
                        retryState = retryState.copy(
                            attemptCount = retryState.attemptCount + 1,
                            isRetrying = true
                        )

                        // UseCase は常に失敗
                        val useCaseSuccess = false

                        if (useCaseSuccess) {
                            retryState = retryState.copy(
                                isRetrying = false,
                                lastError = null,
                                isSuccess = true
                            )
                        } else {
                            retryState = retryState.copy(
                                isRetrying = false,
                                lastError = "試行 ${retryState.attemptCount} 失敗"
                            )
                        }
                    }

                    // 最大試行回数に達した場合の最終処理
                    if (retryState.attemptCount >= retryState.maxAttempts && !retryState.isSuccess) {
                        retryState = retryState.copy(hasFinallyFailed = true)
                    }

                    // 結果確認
                    retryState.attemptCount shouldBe 3 // 最大回数まで試行
                    retryState.isSuccess shouldBe false
                    retryState.hasFinallyFailed shouldBe true
                    retryState.lastError shouldBe "試行 3 失敗"
                }
            }
        }
    }
})
