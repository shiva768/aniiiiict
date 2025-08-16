plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.apollo)
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.zelretch.aniiiiiict"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zelretch.aniiiiiict"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // CLIENT_IDは直接埋め込み
        buildConfigField(
            "String",
            "ANNICT_CLIENT_ID",
            "\"9TBFInCwtgcRuVcK-F892iXt8vQmSci6rbAYg3eNHgk\""
        )

        buildConfigField(
            "String",
            "ANILIST_API_URL",
            "\"https://graphql.anilist.co\""
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
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)

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

    // Testing
    testImplementation(libs.coroutines.test)
    testImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
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
    ignoreFailures.set(true)

    // コンソール出力＋レポート
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    ignoreFailures = true
}
