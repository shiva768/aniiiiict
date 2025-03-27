package com.zelretch.aniiiiiict.util

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ErrorLogger {
    private const val TAG = "AniiiiiictError"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun logError(error: Throwable, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val stackTrace = error.stackTraceToString()
        val message = """
            [Error] $timestamp
            Context: $context
            Message: ${error.message}
            StackTrace:
            $stackTrace
        """.trimIndent()
        
        Log.e(TAG, message)
    }

    fun logWarning(message: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = """
            [Warning] $timestamp
            Context: $context
            Message: $message
        """.trimIndent()
        
        Log.w(TAG, logMessage)
    }

    fun logInfo(message: String, context: String) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = """
            [Info] $timestamp
            Context: $context
            Message: $message
        """.trimIndent()
        
        Log.i(TAG, logMessage)
    }
} 