plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aciderix.obbinstaller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aciderix.obbinstaller"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("hub") {
            storeFile = rootProject.file("keys/hub.keystore")
            storePassword = "obbinstaller"
            keyAlias = "hub"
            keyPassword = "obbinstaller"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("hub")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("hub")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        noCompress += listOf("apk", "obb", "keystore")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // bundletool / bouncycastle bring duplicate META-INF entries
            pickFirsts += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
}

// Copy keystore into assets so the runtime re-signer uses the exact same key
// the hub APK itself was signed with.
val copyKeystoreToAssets by tasks.registering(Copy::class) {
    from(rootProject.file("keys/hub.keystore"))
    into(layout.projectDirectory.dir("src/main/assets"))
    rename { "hub.keystore" }
}
tasks.named("preBuild") { dependsOn(copyKeystoreToAssets) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // APK re-signing: Google's official apksig library, runs on-device.
    // (Android already bundles BouncyCastle in the framework, so we don't pull bcprov.)
    implementation("com.android.tools.build:apksig:8.5.2")
}
