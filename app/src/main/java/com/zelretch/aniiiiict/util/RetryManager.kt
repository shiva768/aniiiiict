package com.zelretch.aniiiiict.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min

data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Long = 1000L,
    val maxDelay: Long = 5000L,
    val factor: Double = 2.0
)

/**
 * リトライロジックを提供するユーティリティクラス
 */
class RetryManager @Inject constructor() {
    /**
     * 指定された回数だけ処理をリトライします
     *
     * @param config リトライ設定
     * @param block リトライする処理
     * @return 処理の結果
     */
    suspend fun <T> retry(config: RetryConfig = RetryConfig(), block: suspend () -> T): T {
        var currentDelay = config.initialDelay
        var lastException: IOException? = null

        repeat(config.maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                lastException = e
                Timber.w(e, "RetryManager: Retry attempt ${attempt + 1}/${config.maxAttempts} failed")

                // 最後の試行でない場合のみ待機
                if (attempt < config.maxAttempts - 1) {
                    delay(currentDelay)
                    // 次回の遅延時間を計算（指数バックオフ）
                    currentDelay = min((currentDelay * config.factor).toLong(), config.maxDelay)
                }
            }
        }

        // すべてのリトライが失敗した場合
        throw lastException ?: error("リトライ操作が失敗しました")
    }

    /**
     * タイムアウト付きでリトライを行います
     *
     * @param timeout タイムアウト時間
     * @param timeUnit タイムアウトの単位
     * @param config リトライ設定
     * @param block リトライする処理
     * @return 処理の結果
     */
    @Suppress("unused")
    suspend fun <T> retryWithTimeout(
        timeout: Long,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        config: RetryConfig = RetryConfig(),
        block: suspend () -> T
    ): T = withTimeout(timeUnit.toMillis(timeout)) {
        retry(config, block)
    }
}
