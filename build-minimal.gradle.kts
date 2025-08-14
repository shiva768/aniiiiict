// This is a minimal build configuration for environments with network restrictions
// The full Android build requires access to blocked domains
// Use this configuration when Android repositories are not accessible

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}