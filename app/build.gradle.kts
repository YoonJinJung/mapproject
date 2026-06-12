plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "edu.skku.map.personalproject"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "edu.skku.map.personalproject"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// AGP 9.x가 Kotlin을 내장 처리하므로 kotlin-android 플러그인 별도 선언 불필요.
// kotlin { } 블록으로 JVM 타겟을 설정합니다.
kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Networking: OkHttp3 — Apache 2.0 — https://github.com/square/okhttp
    implementation(libs.okhttp)

    // JSON parsing: Gson — Apache 2.0 — https://github.com/google/gson
    implementation(libs.gson)

    // Local DB: Room — Apache 2.0 — https://developer.android.com/jetpack/androidx/releases/room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines — Apache 2.0 — https://github.com/Kotlin/kotlinx.coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle — Apache 2.0 — https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(libs.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
