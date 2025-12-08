plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.example.scanview"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)   // Рекомендуемый современный способ, НЕ deprecated
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
}
