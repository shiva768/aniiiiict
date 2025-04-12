import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.apollo)
}

val localProperties = Properties().apply {
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.zelretch.aniiiiiict"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zelretch.aniiiiiict"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField(
            "String",
            "ANNICT_CLIENT_ID",
            "\"${localProperties.getProperty("ANNICT_CLIENT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "ANNICT_CLIENT_SECRET",
            "\"${localProperties.getProperty("ANNICT_CLIENT_SECRET", "")}\""
        )
        buildConfigField(
            "String",
            "ANNICT_ACCESS_TOKEN",
            "\"${localProperties.getProperty("ANNICT_ACCESS_TOKEN", "")}\""
        )

        // DEBUGフラグを手動で設定
//        buildConfigField("boolean", "DEBUG", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit & OkHttp
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.okhttp)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Apollo Client
    implementation(libs.bundles.apollo)

    // Testing
    testImplementation(libs.bundles.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
    debugImplementation(libs.bundles.compose.debug)
}

apollo {
    service("annict") {
        packageName.set("com.zelretch.aniiiiiict")
        schemaFile.set(file("src/main/graphql/schema.json.graphqls"))
        generateKotlinModels.set(true)
        generateFragmentImplementations.set(true)
    }
}