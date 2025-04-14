package com.zelretch.aniiiiiict.util

class TestLogger : Logger {
    override fun logError(tag: String, error: Throwable, context: String) {
        // テスト時はログ出力は不要
    }

    override fun logError(tag: String, errorMessage: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun logWarning(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun logInfo(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun logDebug(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }
} 