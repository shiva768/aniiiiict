plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.apollo) apply false
}

// Note: Static analysis tools (detekt, ktlint) are configured in individual modules
// to avoid dependency resolution issues in environments with limited network access.
// See app/build.gradle.kts for static analysis configuration.

