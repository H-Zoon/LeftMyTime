object Applications {
    // APP Config
    const val minSdk = 26
    const val targetSdk = 33
    const val compileSdk = 33
    const val jvmTarget = "1.8"
    const val versionCode = 1
    const val majorVersion = 1
    const val minorVersion = 0
    const val patchVersion = 0
    const val versionName = "$majorVersion.$minorVersion"
}

object Versions {

    // AndroidX
    const val APP_COMPAT = "1.4.1"
    const val MATERIAL = "1.5.0"
    const val CONSTRAINT_LAYOUT = "2.1.3"

    // KTX
    const val CORE = "1.7.0"

    // TEST
    const val JUNIT = "1.1.3"

    // Android Test
    const val ESPRESSO_CORE = "3.4.0"
}

object Libraries {

    object Compose {

        const val MATERIAL3 = "androidx.compose.material3:material3"
        const val COMPOSE_PREVIEW = "androidx.compose.ui:ui-tooling-preview"
    }

    object AndroidX {
        const val APP_COMPAT = "androidx.appcompat:appcompat:${Versions.APP_COMPAT}"
        const val MATERIAL = "com.google.android.material:material:${Versions.MATERIAL}"
        const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINT_LAYOUT}"
    }

    object KTX {
        const val CORE = "androidx.core:core-ktx:${Versions.CORE}"
    }

    object Test {
        const val JUNIT = "androidx.test.ext:junit:${Versions.JUNIT}"
    }

    object AndroidTest {
        const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:${Versions.ESPRESSO_CORE}"
    }

}