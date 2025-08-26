package com.zelretch.aniiiiict.ui.base

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent

class DefaultCustomTabsIntentFactory : CustomTabsIntentFactory {
    override fun create(): CustomTabsIntent {
        val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(
            true
        ).setUrlBarHidingEnabled(false).build()
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return customTabsIntent
    }
}
