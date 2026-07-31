import org.gradle.buildconfiguration.tasks.UpdateDaemonJvm
import org.gradle.platform.Architecture
import org.gradle.platform.BuildPlatformFactory
import org.gradle.platform.OperatingSystem

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    // Adoptium が配布する platform だけを URL 生成対象にし、Linux 用 JDK を unsupported OS に
    // 誤って割り当てない。platform 一覧は更新時に Adoptium の supported platforms と照合する。
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
