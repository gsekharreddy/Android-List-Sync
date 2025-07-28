pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    // This setting tells Gradle to use the repositories below for all needs,
    // including downloading the Java toolchain (JDK). This is the key fix.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

// The toolchainManagement block has been removed as it was causing the error
// and is no longer necessary with the PREFER_SETTINGS mode.

rootProject.name = "List Sync"
include(":app")
