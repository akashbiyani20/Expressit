plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.whispercpp"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        ndk {
            // Real phones only — keeps the native build fast and the APK lean.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    externalNativeBuild {
        cmake { path = file("src/main/jni/whisper/CMakeLists.txt") }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
