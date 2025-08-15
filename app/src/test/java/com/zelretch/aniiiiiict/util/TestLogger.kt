package com.zelretch.aniiiiiict.util

class TestLogger : Logger {
    override fun error(tag: String, error: Throwable, context: String) {
        // テスト時はログ出力は不要
    }

    override fun error(tag: String, errorMessage: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun warning(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun info(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }

    override fun debug(tag: String, message: String, context: String) {
        // テスト時はログ出力は不要
    }
}
