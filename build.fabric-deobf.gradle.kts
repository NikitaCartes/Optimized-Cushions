plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

// Tag this node so [fabric."<version>"] properties.toml entries resolve as bare property(...) lookups.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}


base.archivesName = "${property("mod_id")}-fabric-mc${property("minecraft_version")}"

repositories {
    maven("https://api.modrinth.com/maven")
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create(property("mod_id") as String) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run"
    }
}

// j52j strips json5 comments from the .json5 mixin configs (Stonecutter preprocesses their per-version
// entries) into the .json that fabric.mod.json references. applyMixinConfig=false: they're listed by hand.
fletchingTable {
    fabric { applyMixinConfig = false }
    j52j.register("main") { extension("json", "optimizedcushions.mixins.json5") }
    j52j.register("client") { extension("json", "optimizedcushions.client.mixins.json5") }
}

dependencies {
    // 26.x snapshots ship Mojang-deobfuscated, so no `mappings(...)` and plain `implementation`
    // (matches EasyAuth's build.fabric-deobf.gradle.kts). Obfuscated versions (<=1.21.11) use a
    // separate build.fabric-obf.gradle.kts with officialMojangMappings + modImplementation.
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Dev-runtime test mods (not shipped): the mod we optimise, plus Sodium when -Pwith_sodium=true.
    // 26.x runs Mojang-native at runtime (no Loom remap), so the loader maps these — plain runtimeOnly.
    runtimeOnly(fletchingTable.modrinth("cushions-backport", property("minecraft_version") as String, "fabric"))
    if (findProperty("with_sodium") == "true") {
        runtimeOnly(fletchingTable.modrinth("sodium", property("minecraft_version") as String, "fabric"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

// Resolve project properties outside the task lambda: inside `tasks.processResources { }`
// the delegate is the task, whose `property(...)` does not see project properties.
val modVersion = property("mod_version")
val supportedMc = property("supported_minecraft_version")
tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("supported_minecraft_version", supportedMc)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to modVersion, "supported_minecraft_version" to supportedMc))
    }
}
