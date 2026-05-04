plugins {
    id("com.android.application")
}

android {
    namespace = "com.aciderix.obbbootstrap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aciderix.obbbootstrap"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
