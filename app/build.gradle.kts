import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.RocTech.musicswitcher"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.RocTech.musicswitcher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    //implementation(fileTree("libs") { include("*.jar") })
    //implementation("com.amap.api:3dmap-location-search:latest.integration")
    //implementation("com.amap.api:search:latest.integration")
    //implementation("com.amap.api:location:latest.integration")
    implementation("com.amap.api:3dmap-location-search:10.1.600_loc6.5.1_sea9.7.4")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}