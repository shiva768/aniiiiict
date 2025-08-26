package com.zelretch.aniiiiict.ui.base

import androidx.browser.customtabs.CustomTabsIntent

interface CustomTabsIntentFactory {
    fun create(): CustomTabsIntent
}
