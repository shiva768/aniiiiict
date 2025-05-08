package com.zelretch.aniiiiiict.ui.base

import androidx.browser.customtabs.CustomTabsIntent

interface CustomTabsIntentFactory {
    fun create(): CustomTabsIntent
}