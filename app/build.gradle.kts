plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.listsync"
    // Using a recent stable SDK version. 36 is not yet released.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.listsync"
        minSdk = 26
        // It's best practice to keep targetSdk the same as compileSdk.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // VERSION_1_8 is a very stable and widely compatible choice for Android.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // Enabling ViewBinding is a modern best practice that replaces findViewById
    // and makes your code safer and easier to read.
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Import the Firebase BoM (Bill of Materials).
    // This automatically manages the versions of all Firebase libraries to ensure they are compatible.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Standard AndroidX and UI Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase SDKs needed for this project.
    // Versions are handled by the BoM, so you don't specify them here.
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Add Glide for efficient image loading from Firebase Storage URLs.
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
