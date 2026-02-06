pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "boxlite-java"

include(":sdk-core")
include(":sdk-native-loader")
include(":sdk-highlevel")
include(":samples:smoke")
include(":samples:simplebox-basic")
include(":samples:simplebox-reuse")
