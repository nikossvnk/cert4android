plugins {
    id("com.android.library")
}

android {
    namespace = "at.bitfire.cert4android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }

    lint {
        targetSdk = 36
        // UseTomlInstead: this library is a separate repository without a version catalog
        disable += setOf("MissingTranslation", "ExtraTranslation", "UseTomlInstead")
    }

    testOptions {
        targetSdk = 36
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.conscrypt:conscrypt-android:2.9.0")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver3:5.5.1")

    testImplementation("junit:junit:4.13.2")
}
