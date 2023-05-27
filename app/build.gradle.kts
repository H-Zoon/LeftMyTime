plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.devidea.timeleft"

    compileSdk = Applications.compileSdk

    defaultConfig {
        minSdk = Applications.minSdk
        targetSdk = Applications.targetSdk
        versionCode = Applications.versionCode
        versionName = Applications.versionName
        setProperty("archivesBaseName", "$applicationId-v$versionName($versionCode)")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        //noinspection DataBindingWithoutKapt
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }
    buildTypes {
        release {
            isDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(platform("androidx.compose:compose-bom:2023.05.01"))
    implementation("androidx.compose.material3:material3")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Optional - Add full set of material icons
    implementation("androidx.compose.material:material-icons-extended")
    // Optional - Add window size utils
    implementation("androidx.compose.material3:material3-window-size-class")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    //implementation("androidx.legacy:legacy-support-v4:1.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //implementation("me.relex:circleindicator:2.1.1")

    kapt("com.android.databinding:compiler:3.1.4")

    //room
    implementation("androidx.room:room-runtime:2.5.1")
    ksp("androidx.room:room-compiler:2.5.1")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:2.5.1")

    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    implementation("com.github.hannesa2:AndroidSlidingUpPanel:4.2.2")

    //implementation"org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlin_version"

    implementation("androidx.activity:activity-ktx:1.7.2")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    // Setting
    implementation("androidx.preference:preference-ktx:2.5.0")
}

