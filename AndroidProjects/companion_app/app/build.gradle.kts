
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.companion"
    compileSdk = 36   // keep stable

    defaultConfig {
        applicationId = "com.example.companion"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // 1. Manually load the local.properties file
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }

// 2. Read the key from the loaded properties
        val mapsKey = localProperties.getProperty("MAPS_API_KEY") ?: ""

// 3. Inject it into the Manifest
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)



    //Authentication
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Navigation (ONLY ONE)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Maps & Location
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase (using BOM correctly)
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
        // Official Google Maps Compose Library
    implementation("com.google.maps.android:maps-compose:4.3.0")
        // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")

}