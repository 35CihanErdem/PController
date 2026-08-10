plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cihan.pccontroller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cihan.pccontroller"
        minSdk = 28  // BluetoothHidDevice API requires Android 9 (API 28)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PRO_PACKAGE", "\"com.cihan.pccontroller.pro\"")
    }

    flavorDimensions += "edition"
    productFlavors {
        create("free") {
            dimension = "edition"
            applicationId = "com.cihan.pccontroller"
            buildConfigField("boolean", "IS_PRO_APP", "false")
        }
        create("pro") {
            dimension = "edition"
            applicationId = "com.cihan.pccontroller.pro"
            buildConfigField("boolean", "IS_PRO_APP", "true")
        }
    }

    buildTypes {
        debug {
            // true yaparsan debug’da Pro kilidi hiç görünmez (yalnız pro flavor’da anlamlı)
            buildConfigField("boolean", "DEBUG_FORCE_PRO", "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEBUG_FORCE_PRO", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.media:media:1.7.1")
    // Billing yalnızca Pro APK’da
    add("proImplementation", "com.android.billingclient:billing-ktx:7.1.1")
}
