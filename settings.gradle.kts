pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://www.jitpack.io") }
        
        // KSP用のリポジトリを追加
        maven { url = uri("https://androidx.dev/snapshots/builds") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://www.jitpack.io") }
        
        // KSP用のリポジトリを追加
        maven { url = uri("https://androidx.dev/snapshots/builds") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
}

rootProject.name = "Aniiiiiict"
include(":app") 