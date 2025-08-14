pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google() // Required for Android Gradle Plugin - may fail if dl.google.com is blocked
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google() // Required for Android dependencies - may fail if dl.google.com is blocked
    }
}

rootProject.name = "Aniiiiiict"
include(":app")