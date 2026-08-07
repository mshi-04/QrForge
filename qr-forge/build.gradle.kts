import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
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

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources(),
            variant = "release",
        ),
    )

    coordinates(
        groupId = "io.github.lambdarc",
        artifactId = "qr-forge",
        version = providers.gradleProperty("VERSION_NAME").orElse("1.0.0-SNAPSHOT").get(),
    )

    publishToMavenCentral()

    pom {
        name.set("QrForge")
        description.set("Rust-powered QR code generation library for Android with a Kotlin API")
        inceptionYear.set("2026")
        url.set("https://github.com/lambdarc/qr-forge")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("lambdarc")
                name.set("lambdarc")
                url.set("https://github.com/lambdarc")
            }
        }

        scm {
            url.set("https://github.com/lambdarc/qr-forge")
            connection.set("scm:git:https://github.com/lambdarc/qr-forge.git")
            developerConnection.set("scm:git:ssh://git@github.com/lambdarc/qr-forge.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/lambdarc/qr-forge/issues")
        }
    }
}
