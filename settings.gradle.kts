pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        // PointHome
        maven { url = uri("https://dl.cloudsmith.io/public/avatye/Android-PointHome/maven/") }
        // AdFit
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
        // Pangle
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        // Cauly
        maven { url = uri("https://cauly.github.io/cauly-sdk-android-maven/maven-repo") }
    }
}

rootProject.name = "TimeLeft"
include(":app")