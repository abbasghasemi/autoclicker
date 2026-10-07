plugins {
    id("com.android.application")
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(rootProject.extra["keyPath"] as String)
            storePassword = rootProject.extra["keyPassword"] as String
            keyAlias = rootProject.extra["keyAlias"] as String
            keyPassword = rootProject.extra["keyPassword"] as String
        }
    }
    namespace = "ghasemi.abbas.autoclicker"
    compileSdk = 37

    defaultConfig {
        applicationId = "ghasemi.abbas.autoclicker"
        minSdk = 24
        targetSdk = 37
        versionCode = 40
        versionName = "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    viewBinding {
        enable = true
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    flavorDimensions += listOf("market")
    productFlavors {
        create("cafebazaar") {
            dimension = "market"
        }
        create("myket") {
            dimension = "market"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("com.github.abbasghasemi:easy-recyclerview-adapter:1.1.4")
}
