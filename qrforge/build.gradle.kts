import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
}

ktlint {
    // KtlintExtension は project ごとに独立しており、root の version 指定は伝播しない。
    version.set(libs.versions.ktlint.cli.get())
}

android {
    namespace = "com.appvoyager.qrforge"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf(
                "arm64-v8a",
                "armeabi-v7a",
                "x86_64",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
