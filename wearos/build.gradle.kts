plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    alias(libs.plugins.hilt)
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "info.proteo.cupcake.wearos"
    compileSdk = 35

    defaultConfig {
        applicationId = "info.proteo.cupcake.wearos"
        minSdk = 30 // Wear OS has a higher minimum SDK version
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":communication")) // Add communication module dependency

    // Wear OS core libraries
    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.phone.interactions)

    // Wear UI components
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.navigation)

    // Android core
    implementation(libs.androidx.core.ktx)

    // Hilt for dependency injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.wearable)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
