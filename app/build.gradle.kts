plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.yape.yapenotificaciones"
    compileSdk = 34

    defaultConfig {
        namespace = "com.yape.yapenotificaciones"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Kotlin 1.9.24 → Compose Compiler 1.5.14
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Compose BOM (estable y compatible con Kotlin 1.9.24 / Compiler 1.5.14)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // Room + KSP
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Apache POI para .xlsx
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Compat / core
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("com.google.android.material:material:1.12.0") // <-- Necesario para Theme.Material3.*
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // <-- solo si usarás activity_main.xml
    implementation("org.apache.poi:poi-ooxml-lite:5.2.5")

    implementation("androidx.compose.material:material-icons-extended")


}

