pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.4"
}

// Target matrix (matches leclowndu93150's Cushion-Backport, the mod this optimises):
//   Fabric  : 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2
//   NeoForge: 1.21.1, 1.21.11, 26.1.2, 26.2
//   Forge   : 1.18.2, 1.19.2, 1.20.1
// Node name = "<mc>-<loader>"; the loader suffix drives the fabric/neoforge/forge constants.
//
// Only nodes whose build script and source guards are in place are registered below; the rest are
// added as each loader/era is ported — see ROADMAP.md.
stonecutter {
    create(rootProject) {
        // Deobfuscated Fabric (26.x snapshots ship Mojang-native jars); 26.2 is the reference node.
        listOf("26.1.2", "26.2").forEach { mc ->
            versions("$mc-fabric" to mc).buildscript("build.fabric-deobf.gradle.kts")
        }
        // Obfuscated Fabric (remapped against Mojmap). 1.21.11/1.21.1 are modern-server, 1.20.1 legacy.
        listOf("1.21.11", "1.21.1", "1.20.1").forEach { mc ->
            versions("$mc-fabric" to mc).buildscript("build.fabric-obf.gradle.kts")
        }
        // NeoForge (ModDevGradle, Mojang-mapped at runtime). 1.21.1+ only — NeoForge does not exist for
        // 1.20.1 and below (that tier is Forge). Cushion-Backport ships NeoForge for these versions.
        listOf("26.2", "26.1.2", "1.21.11", "1.21.1").forEach { mc ->
            versions("$mc-neoforge" to mc).buildscript("build.neoforge.gradle.kts")
        }
        vcsVersion = "26.2-fabric"
    }
}

rootProject.name = "optimizedcushions-backport"
