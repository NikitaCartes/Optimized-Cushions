plugins {
    id("java")
    id("fabric-loom") version "1.17-SNAPSHOT"
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
    // Our mixins target the Cushion-Backport classes by name and inject Minecraft methods they *override*
    // (render / extractRenderState / submit). tiny-remapper only rewrites names it resolves directly on the
    // target, so it leaves those overrides named — fine in the named dev env, broken against obfuscated
    // production. The legacy Mixin AP walks the class hierarchy and emits a refmap with the intermediary
    // names, which Mixin applies at runtime. (Deobf 26.x is Mojang-native at runtime, so it needs none.)
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("optimizedcushions.refmap.json")
    }
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run"
    }
    // Dev convenience: -Pquickplay=<world> makes runClient join that singleplayer world directly.
    findProperty("quickplay")?.let { world ->
        runConfigs["client"].programArgs("--quickPlaySingleplayer", world as String)
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
    // Obfuscated MC (<=1.21.11): remap against Mojang mappings, use modImplementation.
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // The mod we optimise (+ Sodium when -Pwith_sodium=true). Not shipped, but cushions-backport must be
    // on the *compile* classpath, not just runtime: our mixins target its Cushion/CushionRenderer by name
    // and inject inherited Minecraft methods (setPos, extractRenderState…). On obfuscated nodes Loom only
    // rewrites those annotation names to intermediary if it can resolve the target class — modRuntimeOnly
    // leaves it off the remap classpath, so the names stay named and every apply fails in production.
    modImplementation(fletchingTable.modrinth("cushions-backport", property("minecraft_version") as String, "fabric"))
    if (findProperty("with_sodium") == "true") {
        modRuntimeOnly(fletchingTable.modrinth("sodium", property("minecraft_version") as String, "fabric"))
    }
}

// Bytecode target = the node's MC Java floor: 1.20.5+ runs Java 21, older runs 17.
// (26.x nodes use build.fabric-deobf.gradle.kts, which is Java 25 across the board.)
// No toolchain: every node compiles on the build JDK and only `--release` differs, so dev runs fork
// that JDK too — loader 0.19.3 runs 1.20.1 on Java 25 fine.
val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) 21 else 17

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

// `gradlew collectJars` on the root runs this in every node, gathering all jars into build/libs.
// Obfuscated nodes ship the remapped jar, not the dev jar.
tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.remapJar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}

// Loom's dev env pulls LWJGL 3.3.2 over vanilla 1.20.1's 3.3.1, and Sodium 0.5's EarlyDriverScanner
// hard-requires the vanilla version — pin the dev run back to it so runClient works with Sodium.
if (stonecutter.eval(stonecutter.current.version, "<1.20.2")) {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.lwjgl") {
                useVersion("3.3.1")
            }
        }
    }
}

// Resolve project properties outside the task lambda (see build.fabric-deobf.gradle.kts).
val modVersion = property("mod_version")
val supportedMc = property("supported_minecraft_version")
tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("supported_minecraft_version", supportedMc)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to modVersion, "supported_minecraft_version" to supportedMc))
    }
}
