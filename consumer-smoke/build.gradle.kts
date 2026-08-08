plugins {
    id("com.android.application")
}

val consumedVersion = providers.gradleProperty("QR_FORGE_VERSION").orNull
    ?: throw GradleException("QR_FORGE_VERSION must be provided with -P.")

android {
    namespace = "io.github.lambdarc.smoke"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.lambdarc.smoke"
        minSdk = 28
        targetSdk { version = release(37) }
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.github.lambdarc:qr-forge:$consumedVersion")
}
