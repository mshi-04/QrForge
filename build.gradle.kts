import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.platform.Architecture
import org.gradle.platform.BuildPlatformFactory
import org.gradle.platform.OperatingSystem

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish) apply false
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    toolchainPlatforms.set(
        listOf(
            BuildPlatformFactory.of(Architecture.AARCH64, OperatingSystem.LINUX),
            BuildPlatformFactory.of(Architecture.X86_64, OperatingSystem.LINUX),
            BuildPlatformFactory.of(Architecture.AARCH64, OperatingSystem.MAC_OS),
            BuildPlatformFactory.of(Architecture.X86_64, OperatingSystem.MAC_OS),
            BuildPlatformFactory.of(Architecture.X86_64, OperatingSystem.WINDOWS),
        ),
    )
}
