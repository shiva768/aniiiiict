package com.zelretch.aniiiiiict.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min

/**
 * リトライロジックを提供するユーティリティクラス
 */
class RetryManager @Inject constructor(
    private val logger: Logger
) {
    companion object {
        private const val TAG = "RetryManager"
    }

    /**
     * 指定された回数だけ処理をリトライします
     *
     * @param maxAttempts 最大リトライ回数
     * @param initialDelay 初回リトライまでの遅延時間（ミリ秒）
     * @param maxDelay 最大遅延時間（ミリ秒）
     * @param factor 遅延時間の増加係数
     * @param block リトライする処理
     * @return 処理の結果
     */
    suspend fun <T> retry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 5000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                logger.logError(
                    TAG,
                    "リトライ失敗 (${attempt + 1}/$maxAttempts): ${e.message}",
                    "retry"
                )

                // 最後の試行でない場合のみ待機
                if (attempt < maxAttempts - 1) {
                    delay(currentDelay)
                    // 次回の遅延時間を計算（指数バックオフ）
                    currentDelay = min((currentDelay * factor).toLong(), maxDelay)
                }
            }
        }

        // すべてのリトライが失敗した場合
        throw lastException ?: IllegalStateException("リトライが失敗しました")
    }

    /**
     * タイムアウト付きでリトライを行います
     *
     * @param timeout タイムアウト時間
     * @param timeUnit タイムアウトの単位
     * @param maxAttempts 最大リトライ回数
     * @param initialDelay 初回リトライまでの遅延時間（ミリ秒）
     * @param maxDelay 最大遅延時間（ミリ秒）
     * @param factor 遅延時間の増加係数
     * @param block リトライする処理
     * @return 処理の結果
     */
    suspend fun <T> retryWithTimeout(
        timeout: Long,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        maxAttempts: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 5000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T = withTimeout(timeUnit.toMillis(timeout)) {
        retry(maxAttempts, initialDelay, maxDelay, factor, block)
    }
} 