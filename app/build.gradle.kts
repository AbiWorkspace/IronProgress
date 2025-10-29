import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.diets"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.diets"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- FIRMA DE RELEASE (lee keystore.properties en la raíz del proyecto) ---
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                val f = rootProject.file("keystore.properties")
                if (f.exists()) load(f.inputStream())
            }
            storeFile = rootProject.file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            // Firma de release
            signingConfig = signingConfigs.getByName("release")

            // Config de tu release actual
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // debug usa la firma debug por defecto
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}



dependencies {
    // Desugaring (LocalDate, etc.)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // === OkHttp (Fijo para Android 15) ===
    // Usa BOM + artefacto Android y el interceptor.
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.0"))
    implementation("com.squareup.okhttp3:okhttp-android")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // JSON
    implementation("com.google.code.gson:gson:2.8.8")

    // AndroidX / UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")

    // Firebase (todo por BOM, sin versiones explícitas)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.6.0")

    // Glide (sin duplicados)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // ML Kit
    implementation("com.google.mlkit:translate:17.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Add the dependencies for the Crashlytics NDK and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-analytics")
}
