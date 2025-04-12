package com.zelretch.aniiiiiict.util

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AniiiiiictLogger {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun logError(tag: String, error: Throwable, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val stackTrace = error.stackTraceToString()
        val message = """
            [Error] $timestamp
            Context: $context
            Message: ${error.message}
            StackTrace:
            $stackTrace
        """.trimIndent()

        Log.e(tag, message)
    }

    fun logError(tag: String, errorMessage: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val message = """
            [Error] $timestamp
            Context: $context
            Message: $errorMessage
        """.trimIndent()

        Log.e(tag, message)
    }

    fun logWarning(tag: String, message: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = """
            [Warning] $timestamp
            Context: $context
            Message: $message
        """.trimIndent()

        Log.w(tag, logMessage)
    }

    fun logInfo(tag: String, message: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = """
            [Info] $timestamp
            Context: $context
            Message: $message
        """.trimIndent()

        Log.i(tag, logMessage)
    }

    fun logDebug(tag: String, message: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = """
            [Debug] $timestamp
            Context: $context
            Message: $message
        """.trimIndent()

        Log.d(tag, logMessage)
    }
} 