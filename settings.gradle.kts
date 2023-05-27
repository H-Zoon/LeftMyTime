pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://jitpack.io")
        maven("https://maven.google.com/")
        mavenCentral()
    }
}

rootProject.name = "TimeLeft"
include (":app")
