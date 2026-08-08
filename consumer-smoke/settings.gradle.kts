pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // 利用者と同じ解決経路を試すため、この build はルートの composite に含めない。AGP の版だけは
    // ずれないよう version catalog から読む。
    val androidGradlePluginVersion = file("../gradle/libs.versions.toml")
        .readLines()
        .first { it.trimStart().startsWith("agp") }
        .substringAfter('"')
        .substringBefore('"')

    plugins {
        id("com.android.application") version androidGradlePluginVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer-smoke"
