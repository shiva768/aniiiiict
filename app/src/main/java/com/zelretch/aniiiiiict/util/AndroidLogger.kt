package com.zelretch.aniiiiiict.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLogger @Inject constructor() : Logger {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private fun formatMessage(message: String, context: String): String {
        val timestamp = dateFormat.format(Date())
        return "[$timestamp][$context] $message"
    }

    override fun error(tag: String, error: Throwable, context: String) {
        Log.e(tag, formatMessage(error.message ?: "Unknown error", context), error)
    }

    override fun error(tag: String, errorMessage: String, context: String) {
        Log.e(tag, formatMessage(errorMessage, context))
    }

    override fun warning(tag: String, message: String, context: String) {
        Log.w(tag, formatMessage(message, context))
    }

    override fun info(tag: String, message: String, context: String) {
        Log.i(tag, formatMessage(message, context))
    }

    override fun debug(tag: String, message: String, context: String) {
        Log.d(tag, formatMessage(message, context))
    }
} 