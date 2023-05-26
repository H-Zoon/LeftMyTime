plugins {
    id ("com.android.application")
    id ("kotlin-android")
    id ("kotlin-kapt")
    id ("org.jetbrains.kotlin.android")
}

android {
    compileSdkVersion(Apps.compileSdk)
    defaultConfig {
        minSdkVersion(Apps.minSdk)
        targetSdkVersion(Apps.targetSdk)
        versionCode = Apps.versionCode
        versionName = Apps.versionName
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

    defaultConfig {
        applicationId = "com.devidea.timeleft"
        minSdkVersion(26)
        targetSdkVersion(33)
        versionCode(14)
        versionName(5.5)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            debuggable = false
            shrinkResources = true
            minifyEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    namespace ="com.devidea.timelef"

}

dependencies {
    implementation platform "androidx.compose:compose-bom:2023.01.0"
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

    implementation = "androidx.appcompat:appcompat:1.6.1"
    implementation = "com.google.android.material:material:1.9.1"
    implementation = "androidx.constraintlayout:constraintlayout:2.1.1"
    implementation = "androidx.legacy:legacy-support-v4:1.0.1"
    testImplementation = "junit:junit:4.13.1"
    androidTestImplementation = "androidx.test.ext:junit:1.1.1"
    androidTestImplementation = "androidx.test.espresso:espresso-core:3.5.1"
    implementation = "me.relex:circleindicator:2.1.1"

    kapt = "com.android.databinding:compiler:3.1.4"

    //room
    implementation = "androidx.room:room-runtime:2.5.1"
    ksp = "androidx.room:room-compiler:2.5.1"

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation = "androidx.room:room-ktx:2.5.1"

    //coroutines
    implementation = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1"
    implementation = "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0"

    implementation = "com.github.hannesa2:AndroidSlidingUpPanel:4.2.1"

    //implementation"org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlin_version"

    implementation = "androidx.activity:activity-ktx:1.7.1"

    val lifecycle_version = "2.4.0"

    // ViewModel
    implementation = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
    // LiveData
    implementation = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"
    // Setting
    implementation = "androidx.preference:preference-ktx:1.2."
}

