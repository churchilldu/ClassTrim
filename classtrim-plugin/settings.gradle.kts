rootProject.name = "classtrim-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    // Toolchain-aware download of the JBR / JDK matching the IntelliJ Platform.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
