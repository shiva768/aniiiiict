package com.zelretch.aniiiiiict.util

interface Logger {
    fun logError(tag: String, error: Throwable, context: String)
    fun logError(tag: String, errorMessage: String, context: String)
    fun logWarning(tag: String, message: String, context: String)
    fun logInfo(tag: String, message: String, context: String)
    fun logDebug(tag: String, message: String, context: String)
} 