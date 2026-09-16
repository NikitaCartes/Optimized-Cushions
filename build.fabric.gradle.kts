plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
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

dependencies {
    // 26.x ships Mojang-deobfuscated, so no `mappings(...)` and plain `implementation`.
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
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
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
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
