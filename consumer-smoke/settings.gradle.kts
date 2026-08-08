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
        .firstOrNull { it.substringBefore('=').trim() == "agp" }
        ?.substringAfter('=')
        ?.trim()
        ?.removeSurrounding("\"")
        ?.takeIf { it.isNotBlank() }
        ?: error("agp version was not found in gradle/libs.versions.toml.")

    plugins {
        id("com.android.application") version androidGradlePluginVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 検証対象は直前に publish した AAR に限る。fallback を許すと、同じ座標が Maven Central に
        // ある version では公開済みの成果物を検証してしまう。
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("io.github.lambdarc") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer-smoke"
