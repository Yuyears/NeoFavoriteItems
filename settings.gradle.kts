pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.parchmentmc.org/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.architectury.loom") {
                useModule("dev.architectury:architectury-loom:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

listOf(
    "common",
    "fabric",
    "forge",
    "neoforge"
).forEach { include(it) }
