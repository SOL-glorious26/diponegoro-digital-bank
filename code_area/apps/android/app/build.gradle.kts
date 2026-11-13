import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Kunci penandatangan APK rilis dibaca dari keystore.properties (tidak di-commit).
// Bila berkas tidak ada, build rilis tetap jalan tetapi APK tidak ditandatangani.
val releaseKeystore = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.diponegoro.digitalbank"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.diponegoro.digitalbank"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Fase 5 (DAFTAR_PEKERJAAN 5.2): dua varian aplikasi.
    //  - demo   : data simulasi in-memory (tanpa server), dipakai untuk uji UI & cadangan demo.
    //  - online : memakai backend bersama lewat REST (SAD-DDB-001 ADR-02, ADR-09).
    // URL backend online dapat diganti: ./gradlew assembleOnlineDebug -Pddb.baseUrl=http://192.168.1.10:8080/
    flavorDimensions += "backend"
    productFlavors {
        create("demo") {
            dimension = "backend"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "ONLINE", "false")
            buildConfigField("String", "BASE_URL", "\"\"")
        }
        create("online") {
            dimension = "backend"
            val baseUrl = (project.findProperty("ddb.baseUrl") as String?) ?: "http://10.0.2.2:8080/"
            buildConfigField("boolean", "ONLINE", "true")
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            // HTTP polos untuk backend lokal diizinkan lewat src/online/res/xml/network_security_config.xml.
        }
    }

    signingConfigs {
        if (!releaseKeystore.isEmpty) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (!releaseKeystore.isEmpty) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Robolectric: uji UI Compose end-to-end di JVM (tanpa emulator).
        unitTests.isIncludeAndroidResources = true
        // Uji end-to-end ke backend sungguhan (opsional): ./gradlew testOnlineDebugUnitTest -Pddb.liveUrl=http://localhost:8080/
        unitTests.all { test ->
            (project.findProperty("ddb.liveUrl") as String?)?.let { test.systemProperty("ddb.liveUrl", it) }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Ikon UI (dashboard & menu)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // QRIS: generate & baca kode QR (dipakai juga oleh modul Payment versi lama)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Fase 5: klien REST ke backend bersama
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)

    // Uji UI end-to-end + screenshot di JVM
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}