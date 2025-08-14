plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.apollo) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Detekt configuration
detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

// KtLint configuration
ktlint {
    version.set("1.0.1")
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
}

// Apply to all subprojects
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

// Global task to run all static analysis
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Run all static analysis tools"
    dependsOn("detekt", "ktlintCheck")
}

