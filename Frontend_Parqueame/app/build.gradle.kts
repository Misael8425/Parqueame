plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"

    // ✅ Añadido para usar @Parcelize
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.parqueame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.parqueame"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))

    // UI Compose base (sin versión: la resuelve el BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navegación/Activity/Lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // (Opcional) accompanist
    implementation(libs.accompanistSystemUiController)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation(libs.androidx.compose.foundation)

    // Test/Debug
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material:material-icons-extended")

    // --- Serialización JSON ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // --- DataStore ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Retrofit / OkHttp ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // --- Maps / Location / Places ---
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("androidx.core:core-ktx:1.12.0")

    // --- Coil ---
    implementation("io.coil-kt:coil-compose:2.5.0")

    // --- UCrop + AppCompat ---
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // --- Stripe Android SDK ---
    implementation("com.stripe:stripe-android:21.22.1")
    implementation("com.stripe:payments-core:21.22.1")
    implementation("com.stripe:financial-connections:21.22.1")
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // ZXing core + helper de journeyapps para generar el Bitmap del QR
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
