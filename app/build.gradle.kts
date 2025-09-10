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

val annictClientId =
    providers.gradleProperty("ANNICT_CLIENT_ID")
        .orElse(providers.environmentVariable("ANNICT_CLIENT_ID"))
val annictClientSecret =
    providers.gradleProperty("ANNICT_CLIENT_SECRET")
        .orElse(providers.environmentVariable("ANNICT_CLIENT_SECRET"))
val malClientId =
    providers.gradleProperty("MAL_CLIENT_ID")
        .orElse(providers.environmentVariable("MAL_CLIENT_ID"))
val isCi = providers.environmentVariable("CI").isPresent
val isCheckOnly = gradle.startParameter.taskNames.any { it.contains("check", ignoreCase = true) } &&
    gradle.startParameter.taskNames.none {
        it.contains("assemble", ignoreCase = true) ||
            it.contains("bundle", ignoreCase = true)
    }

// Configuration Cache対応のためにタスクを個別に設定
tasks.withType<Task>().matching {
    it.name.startsWith("assemble") || it.name.startsWith("bundle")
}.configureEach {
    val taskAnnictClientId = annictClientId
    val taskAnnictSecret = annictClientSecret
    val taskMalClientId = malClientId
    val taskIsCi = isCi
    val taskIsCheckOnly = isCheckOnly

    doFirst {
        val isRelease = name.contains("Release", ignoreCase = true)
        val isDebug = name.contains("Debug", ignoreCase = true)
        val requireSecret = !taskIsCheckOnly && (isRelease || (isDebug && taskIsCi))
        val idValue = taskAnnictClientId.orNull
        val secretValue = taskAnnictSecret.orNull
        val malIdValue = taskMalClientId.orNull

        if (requireSecret && idValue.isNullOrEmpty() && secretValue.isNullOrEmpty() && malIdValue.isNullOrEmpty()) {
            throw GradleException(
                "ANNICT_CLIENT_ID and ANNICT_CLIENT_SECRET and MAL_CLIENT_ID " +
                    "environment variable is required for " + (if (isRelease) "Release" else "CI Debug") + " builds"
            )
        }
    }
}

android {
    namespace = "com.zelretch.aniiiiict"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zelretch.aniiiiict"
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
            "\"${annictClientId.orElse("").get()}\""
        )

        buildConfigField(
            "String",
            "ANILIST_API_URL",
            "\"https://graphql.anilist.co\""
        )

        buildConfigField(
            "String",
            "MAL_CLIENT_ID",
            "\"${malClientId.orElse("").get()}\""
        )

        // ←ここで BuildConfig に渡す
        buildConfigField(
            "String",
            "ANNICT_CLIENT_SECRET",
            "\"${annictClientSecret.orElse("").get()}\""
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
    ignoreFailures = true
}
