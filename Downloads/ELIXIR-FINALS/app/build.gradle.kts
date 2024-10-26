plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.facecoloranalyzer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.facecoloranalyzer"
        minSdk = 26
        targetSdk = 33
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.QuadFlask:colorpicker:0.0.13")
    implementation("com.github.yukuku:ambilwarna:2.0.1")
    implementation("com.github.Dhaval2404:ColorPicker:2.3")
    implementation ("com.google.android.material:material:1.9.0") // or the latest version
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.exifinterface:exifinterface:1.3.3")
    implementation(libs.ui)
    implementation(libs.activity.compose)
    implementation(libs.material3.android)
    implementation(libs.runtime.android)
    implementation(libs.ui.graphics.android)
    implementation(libs.ui.android)
    implementation(libs.foundation.layout.android)
    implementation(libs.core.ktx)
    implementation(libs.ui.tooling.preview.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
