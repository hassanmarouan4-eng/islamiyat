import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: keystore.properties (local, never committed) or SIGNING_* environment variables (CI).
// Without them, release builds are signed with the shared development key in keystore/dev.jks so that
// installed copies can always be updated. Use a private upload key for Google Play.
val releaseSigning = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingProp(key: String, env: String): String? =
    releaseSigning.getProperty(key) ?: System.getenv(env)?.takeIf { it.isNotBlank() }

android {
    namespace = "org.dorossi.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.dorossi.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            val shared = rootProject.file("keystore/dev.jks")
            if (shared.exists()) {
                storeFile = shared
                storePassword = "VLdRBC6YbR0ncZgT2WnM"
                keyAlias = "dev"
                keyPassword = "VLdRBC6YbR0ncZgT2WnM"
            }
        }
        create("release") {
            val path = signingProp("storeFile", "SIGNING_STORE_FILE")
            if (path != null) {
                storeFile = rootProject.file(path)
                storePassword = signingProp("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingProp("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signingProp("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (signingProp("storeFile", "SIGNING_STORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
