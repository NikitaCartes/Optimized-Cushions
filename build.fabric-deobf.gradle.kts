plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
}

// Tag this node so [fabric."<version>"] properties.toml entries resolve as bare property(...) lookups.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

version = property("mod_version").toString()
group = property("maven_group").toString()

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

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "optimizedcushionsbackport-test"
        eula = true
        enableGameTests = true
        enableClientGameTests = true
    }
}

dependencies {
    // 26.x snapshots ship Mojang-deobfuscated, so no `mappings(...)` and plain `implementation`.
    // Obfuscated versions (<=1.21.11) use build.fabric-obf.gradle.kts instead.
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

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "LICENSE_optimizedcushions" }
    }
}

// `gradlew collectJars` on the root runs this in every node, gathering all jars into build/libs.
tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
    dependsOn("build", rootProject.tasks.named("cleanCollectedJars"))
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

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""
    val githubToken = System.getenv("GITHUB_TOKEN") ?: ""

    file = tasks.jar.get().archiveFile
    dryRun = modrinthToken.isEmpty() || curseforgeToken.isEmpty() || githubToken.isEmpty()
    displayName = "${property("display_name")} ${project.version}"
    version = project.version.toString()
    changelog = rootProject.file("RELEASE_NOTE.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    val targets = property("supported_versions").toString().split(",")
    modrinth {
        projectId = "PD2xMNLQ"
        accessToken = modrinthToken
        targets.forEach(minecraftVersions::add)
    }
    curseforge {
        projectId = "1604330"
        accessToken = curseforgeToken
        targets.forEach(minecraftVersions::add)
        client.set(true)
        server.set(true)
    }
    github {
        accessToken = githubToken
        parent(rootProject.tasks.named("publishGithub"))
    }
}
