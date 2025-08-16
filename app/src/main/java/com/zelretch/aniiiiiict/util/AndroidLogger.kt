package com.zelretch.aniiiiiict.util

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        Timber.e(error, formatMessage(error.message ?: "Unknown error", context))
    }

    override fun error(tag: String, errorMessage: String, context: String) {
        Timber.e(formatMessage(errorMessage, context))
    }

    override fun warning(tag: String, message: String, context: String) {
        Timber.w(formatMessage(message, context))
    }

    override fun info(tag: String, message: String, context: String) {
        Timber.i(formatMessage(message, context))
    }

    override fun debug(tag: String, message: String, context: String) {
        Timber.d(formatMessage(message, context))
    }
}
