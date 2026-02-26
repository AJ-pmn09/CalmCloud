plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mindaigle"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mindaigle"
        minSdk = 26  // Health Connect requires 26+; was 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // VM backend port from START_ALL output (e.g. 3009, 3011 when 3003 in use)
        buildConfigField("String", "SERVER_URL", "\"http://192.168.100.6\"")
        buildConfigField("String", "SERVER_PORT", "\"3009\"")
        buildConfigField("boolean", "ENABLE_DEBUG_CONFIG", "false")
    }

    buildTypes {
        debug {
            buildConfigField("String", "SERVER_URL", "\"http://192.168.100.6\"")
            buildConfigField("String", "SERVER_PORT", "\"3009\"")
            buildConfigField("boolean", "ENABLE_DEBUG_CONFIG", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Production server - uses values from defaultConfig above
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.socket.io.client)
    implementation(libs.coil.compose)
    implementation(libs.health.connect)
    implementation(libs.androidx.work.runtime.ktx)
}