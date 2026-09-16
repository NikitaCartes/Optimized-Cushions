pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.4"
}

// Node name = "<mc>-<loader>"; the loader suffix drives the fabric/neoforge constants.
stonecutter {
    create(rootProject) {
        versions("26.3-fabric" to "26.3").buildscript("build.fabric.gradle.kts")
        versions("26.3-neoforge" to "26.3").buildscript("build.neoforge.gradle.kts")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "optimizedcushions"
