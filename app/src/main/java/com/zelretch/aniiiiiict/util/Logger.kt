package com.zelretch.aniiiiiict.util

interface Logger {
    fun error(
        tag: String,
        error: Throwable,
        context: String,
    )

    fun error(
        tag: String,
        errorMessage: String,
        context: String,
    )

    fun warning(
        tag: String,
        message: String,
        context: String,
    )

    fun info(
        tag: String,
        message: String,
        context: String,
    )

    fun debug(
        tag: String,
        message: String,
        context: String,
    )
}
