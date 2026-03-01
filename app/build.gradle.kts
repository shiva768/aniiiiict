import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.apollo)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// local.propertiesから値を読み込む
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val isCi = providers.environmentVariable("CI").isPresent
val isCheckOnly = gradle.startParameter.taskNames.any {
    it.contains("check", ignoreCase = true) ||
        it.contains("test", ignoreCase = true)
} &&
    gradle.startParameter.taskNames.none {
        it.contains("assemble", ignoreCase = true) ||
            it.contains("bundle", ignoreCase = true)
    }

fun getPropertyValue(key: String): String = localProperties.getProperty(key)
    ?: providers.gradleProperty(key).orNull
    ?: providers.environmentVariable(key).orNull
    ?: ""

// check/testのみの場合はdummy値を使用、それ以外（assemble/bundle）では実際の値が必要
val annictClientId = getPropertyValue("ANNICT_CLIENT_ID").ifEmpty {
    if (isCheckOnly) "dummy_ANNICT_CLIENT_ID" else ""
}
val annictClientSecret = getPropertyValue("ANNICT_CLIENT_SECRET").ifEmpty {
    if (isCheckOnly) "dummy_ANNICT_CLIENT_SECRET" else ""
}
val malClientId = getPropertyValue("MAL_CLIENT_ID").ifEmpty {
    if (isCheckOnly) "dummy_MAL_CLIENT_ID" else ""
}

// Configuration Cache対応のためにタスクを個別に設定
// APK/AAB作成タスクのみをチェック（assembleDebug, assembleRelease, bundleDebug, bundleRelease）
tasks.withType<Task>().matching {
    it.name == "assembleDebug" ||
        it.name == "assembleRelease" ||
        it.name == "bundleDebug" ||
        it.name == "bundleRelease"
}.configureEach {
    val taskAnnictClientId = annictClientId
    val taskAnnictSecret = annictClientSecret
    val taskMalClientId = malClientId
    val taskIsCi = isCi
    val taskIsCheckOnly = isCheckOnly

    doFirst {
        val isRelease = name.contains("Release", ignoreCase = true)
        val isDebug = name.contains("Debug", ignoreCase = true)
        val buildType = if (isRelease) "Release" else "Debug"

        // assemble/bundle実行時は値が必須
        if (taskAnnictClientId.isEmpty() || taskAnnictSecret.isEmpty() || taskMalClientId.isEmpty()) {
            throw GradleException(
                """
                |ANNICT_CLIENT_ID, ANNICT_CLIENT_SECRET, and MAL_CLIENT_ID are required for $buildType APK builds.
                |
                |Please set REAL credentials in one of the following ways:
                |  1. local.properties (recommended for local development)
                |     ANNICT_CLIENT_ID=your_real_value
                |     ANNICT_CLIENT_SECRET=your_real_secret
                |     MAL_CLIENT_ID=your_real_value
                |
                |  2. Environment variables
                |     export ANNICT_CLIENT_ID=your_real_value
                |     export ANNICT_CLIENT_SECRET=your_real_secret
                |     export MAL_CLIENT_ID=your_real_value
                |
                |Note: Dummy values are NOT allowed for APK builds.
                |      Use './gradlew check' or './gradlew test' for testing with dummy values.
                """.trimMargin()
            )
        }

        // APK作成時はdummy値を絶対に拒否
        if (taskAnnictClientId.startsWith("dummy_") ||
            taskAnnictSecret.startsWith("dummy_") ||
            taskMalClientId.startsWith("dummy_")
        ) {
            throw GradleException(
                """
                |Dummy values are NOT allowed for APK builds (both Debug and Release).
                |Current values detected: 
                |  ANNICT_CLIENT_ID: ${if (taskAnnictClientId.startsWith("dummy_")) "DUMMY" else "OK"}
                |  ANNICT_CLIENT_SECRET: ${if (taskAnnictSecret.startsWith("dummy_")) "DUMMY" else "OK"}
                |  MAL_CLIENT_ID: ${if (taskMalClientId.startsWith("dummy_")) "DUMMY" else "OK"}
                |
                |Please set REAL credentials in local.properties or environment variables.
                |
                |For testing without building APK, use:
                |  ./gradlew check    (runs tests with dummy values automatically)
                |  ./gradlew test     (runs unit tests with dummy values automatically)
                """.trimMargin()
            )
        }
    }
}

android {
    namespace = "com.zelretch.aniiiiict"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        val baseVersionName = "1.0"
        val buildTimestamp = System.getenv("BUILD_TIMESTAMP")
        val buildRefName = System.getenv("BUILD_REF_NAME")?.replace("/", "-")

        versionName = if (buildTimestamp != null && buildRefName != null) {
            "$baseVersionName-$buildRefName-$buildTimestamp"
        } else {
            baseVersionName
        }

        testInstrumentationRunner = "com.zelretch.aniiiiict.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "ANNICT_CLIENT_ID",
            "\"$annictClientId\""
        )

        buildConfigField(
            "String",
            "ANILIST_API_URL",
            "\"https://graphql.anilist.co\""
        )

        buildConfigField(
            "String",
            "MAL_CLIENT_ID",
            "\"$malClientId\""
        )

        buildConfigField(
            "String",
            "ANNICT_CLIENT_SECRET",
            "\"$annictClientSecret\""
        )

        // DEBUGフラグを手動で設定
//        buildConfigField("boolean", "DEBUG", "false")
    }

    signingConfigs {
        // GHAビルド時のみデバッグ署名設定を適用
        if (System.getenv("CI") != null) {
            getByName("debug").apply {
                storeFile = file("$rootDir/app/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/*.md"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Disable Kotest classpath autoscan to reduce startup overhead
    systemProperty("kotest.framework.classpath.scanning.config.disable", "true")
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.navigation.compose)

    // Compose
    // Align Kotlin libs to the plugin version
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.okhttp.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    // DI (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.okhttp)
    implementation(libs.bundles.apollo)

    // Async
    implementation(libs.coroutines.android)

    // UI
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(kotlin("reflect"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.bundles.compose.debug)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

apollo {
    service("annict") {
        packageName.set("com.annict")
        srcDir("src/main/graphql/com.annict")
        schemaFile.set(file("src/main/graphql/com.annict/schema.json.graphqls"))
        generateKotlinModels.set(true)
        generateFragmentImplementations.set(true)
    }

    service("anilist") {
        packageName.set("co.anilist")
        srcDir("src/main/graphql/co.anilist")
        schemaFile.set(file("src/main/graphql/co.anilist/schema.graphqls"))
        generateKotlinModels.set(true)
        generateFragmentImplementations.set(true)
        // スキーマのダウンロード元（インスペクション設定）
        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            // 認証が必要ならヘッダー追加:
            // headers.put("Authorization", "Bearer xxx")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ktlint {
    // IDE側のエンジン（ログに 1.5.0 と出ている）に合わせる
    version.set("1.5.0")
    // 違反があってもビルド失敗にしない（検出は残す）
    ignoreFailures.set(false)

    // コンソール出力＋レポート
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    ignoreFailures = false
}
