import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

println("🔍 Mencari file keystore.properties untuk PackDroid...")

if (keystorePropertiesFile.exists()) {
    println("✅ File ditemukan di: ${keystorePropertiesFile.absolutePath}")
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    
    // Debug: lihat properti yang terbaca
    println("📋 Isi file properties:")
    keystoreProperties.forEach { key, value ->
        if (key.toString().contains("password", ignoreCase = true)) {
            println("   $key = [PROTECTED]")
        } else {
            println("   $key = $value")
        }
    }
} else {
    throw GradleException("❌ File keystore.properties TIDAK ditemukan! Buat file terlebih dahulu.")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.packdroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.packdroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            println("🔧 Konfigurasi signingConfigs.release untuk PackDroid...")
            
            val storePwd = keystoreProperties.getProperty("storePassword")
            val keyPwd = keystoreProperties.getProperty("keyPassword")
            val keyAliasVal = keystoreProperties.getProperty("keyAlias")
            val storeFileVal = keystoreProperties.getProperty("storeFile")

            // Debug
            println("   storePassword: ${if (storePwd != null) "[ADA]" else "[NULL]"}")
            println("   keyPassword: ${if (keyPwd != null) "[ADA]" else "[NULL]"}")
            println("   keyAlias: ${if (keyAliasVal != null) "[ADA]" else "[NULL]"}")
            println("   storeFile: ${if (storeFileVal != null) "[ADA]" else "[NULL]"}")

            if (storePwd == null || keyPwd == null || keyAliasVal == null || storeFileVal == null) {
                val missing = mutableListOf<String>()
                if (storePwd == null) missing.add("storePassword")
                if (keyPwd == null) missing.add("keyPassword")
                if (keyAliasVal == null) missing.add("keyAlias")
                if (storeFileVal == null) missing.add("storeFile")
                
                throw GradleException("""
                    ❌ Signing config error: Properti berikut tidak ditemukan:
                       ${missing.joinToString(", ")}
                    
                    📁 Lokasi file: ${keystorePropertiesFile.absolutePath}
                    
                    📝 Pastikan isi file:
                       storePassword=xxx
                       keyPassword=xxx
                       keyAlias=xxx
                       storeFile=xxx
                """.trimIndent())
            }

            storePassword = storePwd
            keyPassword = keyPwd
            keyAlias = keyAliasVal
            storeFile = file(storeFileVal)
            
            println("✅ SigningConfig PackDroid berhasil dikonfigurasi")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin { 
        jvmToolchain(11) 
    }

    buildFeatures { 
        compose = true 
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.commons.compress)
    implementation(libs.junrar)
    implementation(libs.sevenzip)
    implementation(libs.sevenzip.all)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ExoPlayer untuk video & audio
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")

    // Coil untuk image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}