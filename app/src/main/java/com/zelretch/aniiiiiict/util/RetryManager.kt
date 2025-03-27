package com.zelretch.aniiiiiict.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile

class RetryManager {
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY = 1000L // 1秒
        private const val MAX_DELAY = 10000L // 10秒
    }

    fun <T> retryWithExponentialBackoff(
        operation: suspend () -> T,
        shouldRetry: (Throwable) -> Boolean = { true }
    ): Flow<T> = flow {
        var currentDelay = INITIAL_DELAY
        var retryCount = 0

        while (true) {
            try {
                emit(operation())
                break
            } catch (e: Throwable) {
                if (!shouldRetry(e) || retryCount >= MAX_RETRIES) {
                    throw e
                }

                ErrorLogger.logWarning(
                    "リトライ実行: ${retryCount + 1}/${MAX_RETRIES}, 遅延: ${currentDelay}ms",
                    "RetryManager"
                )

                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(MAX_DELAY)
                retryCount++
            }
        }
    }
} 