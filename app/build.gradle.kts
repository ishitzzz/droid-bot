plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.droidbot.agent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.droidbot.agent"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Gemini API Key from local.properties
        val localPropsFile = rootProject.file("local.properties")
        val apiKey = if (localPropsFile.exists()) {
            localPropsFile.readLines()
                .firstOrNull { it.startsWith("GEMINI_API_KEY=") }
                ?.substringAfter("=")
                ?.trim() ?: ""
        } else ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
}

dependencies {
    // ──────────────────────────────────────────────
    // Jetpack Compose
    // ──────────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ──────────────────────────────────────────────
    // Lifecycle & ViewModel
    // ──────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    // ──────────────────────────────────────────────
    // Hilt (DI)
    // ──────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.53.1")
    ksp("com.google.dagger:hilt-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ──────────────────────────────────────────────
    // Google Generative AI (Gemini Cloud — Pro)
    // ──────────────────────────────────────────────
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // ──────────────────────────────────────────────
    // Google AI Edge / ML Kit (Gemini Nano — On-Device)
    // ──────────────────────────────────────────────
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // TODO: Add AICore dependency when available for on-device Gemini Nano
    // implementation("com.google.ai.edge.litert:litert:...")

    // ──────────────────────────────────────────────
    // Biometric
    // ──────────────────────────────────────────────
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ──────────────────────────────────────────────
    // Security (EncryptedSharedPreferences)
    // ──────────────────────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ──────────────────────────────────────────────
    // Room (Local cache for UI Maps)
    // ──────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ──────────────────────────────────────────────
    // Ktor (Cloud bridge HTTP client)
    // ──────────────────────────────────────────────
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

    // ──────────────────────────────────────────────
    // Kotlinx Serialization
    // ──────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ──────────────────────────────────────────────
    // Core AndroidX
    // ──────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")

    // ──────────────────────────────────────────────
    // Testing
    // ──────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
