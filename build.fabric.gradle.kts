plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("maven-publish")
}

version = property("mod_version").toString()
group = property("maven_group").toString()

// Tag this node so [fabric."<version>"] properties.toml entries resolve as bare property(...) lookups.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

base.archivesName = "${property("mod_id")}-fabric-mc${property("minecraft_version")}"

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

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "optimizedcushions-test"
        eula = true
        enableGameTests = true
        enableClientGameTests = true
    }
}

// Test-mod jar for production runs: the `gametest` source set has no jar task,
// and the production run below only sees real mod jars. 26.x is
// Mojang-deobfuscated, so no remapping step is needed.
val gametestJar by tasks.registering(org.gradle.api.tasks.bundling.Jar::class) {
    group = "build"
    description = "Assembles the gametest (test-mod) jar for production runs."
    archiveClassifier.set("gametest")
    from(sourceSets["gametest"].output)
}

// Client game tests on headless CI (GitHub Actions): run them through Loom's
// production run task, which manages its own Xvfb display, instead of wrapping
// the dev `runClientGameTest` task in `xvfb-run` (which hangs indefinitely).
// See https://docs.fabricmc.net/develop/automatic-testing#run-game-tests-on-github-actions
tasks.register("runProductionClientGameTest", net.fabricmc.loom.task.prod.ClientProductionRunTask::class) {
    jvmArgs.add("-Dfabric.client.gametest")
    useXVFB.set(true)
    mods.from(gametestJar)
}

dependencies {
    // 26.x ships Mojang-deobfuscated, so no `mappings(...)` and plain `implementation`.
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    // Production-run mods: the CI client game test launches the game like a
    // production launcher, so it needs fabric-api as an installed mod.
    productionRuntimeMods("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
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
tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to modVersion))
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
