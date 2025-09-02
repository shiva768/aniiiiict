package com.zelretch.aniiiiict.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DisableAnimationsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                disableAnimations()
                try {
                    base.evaluate()
                } finally {
                    enableAnimations()
                }
            }
        }
    }

    private fun disableAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global window_animation_scale 0"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global transition_animation_scale 0"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global animator_duration_scale 0"
            )
        }
    }

    private fun enableAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global window_animation_scale 1"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global transition_animation_scale 1"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "settings put global animator_duration_scale 1"
            )
        }
    }
}
