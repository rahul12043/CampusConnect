import java.util.Properties
import java.io.FileInputStream

plugins {


    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}


android {
    buildFeatures{
        compose = true
        buildConfig = true}
    namespace = "com.example.campusconnect"
    compileSdk = 35

    defaultConfig {
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY")}\"")

        buildConfigField("String", "CLOUDINARY_URL", "\"${project.findProperty("CLOUDINARY_URL")}\"")
        applicationId = "com.example.campusconnect"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildFeatures {
        // THIS LINE ENABLES THE BuildConfig FILE
        buildConfig = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            // Your existing rule
            pickFirsts += "META-INF/native-image/reflect-config.json"

            // --- ADD THIS NEW RULE ---
            pickFirsts += "META-INF/native-image/resource-config.json"
        }
    }
}

dependencies {

    // Core Android & Jetpack
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")


    implementation("com.google.ai.client.generativeai:generativeai:0.5.0")

    // Jetpack Compose Bill of Materials (BOM) - Manages versions for all Compose libraries
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation ("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel with Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")



    // Firebase Bill of Materials (BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth") // No need for -ktx anymore
    implementation("com.google.firebase:firebase-firestore") // No need for -ktx anymore
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")

    implementation("com.google.firebase:firebase-common-ktx")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // For Firebase await()

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Maps for Compose (optional, for Lost & Found)
    implementation("com.google.maps.android:maps-compose:5.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation(libs.androidx.appcompat)
    // For loading images from URLs or local URIs
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Coroutines for asynchronous tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

 implementation ("com.cloudinary:cloudinary-android:2.5.0")

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-tooling-internal:1.6.6")
    implementation(libs.androidx.animation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}