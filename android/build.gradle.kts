// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the kotlin-android plugin is NOT applied.
// The Kotlin Gradle plugin is pinned here so that the Kotlin compiler, the Compose compiler plugin
// and the serialization plugin always share one version (the "kotlin" entry in libs.versions.toml).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
