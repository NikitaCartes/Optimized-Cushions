plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.147"
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.22"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
}

// Tag this node so [neoforge."<version>"] properties.toml entries resolve as bare property(...) lookups.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

version = property("mod_version").toString()
group = property("maven_group").toString()

base.archivesName = "${property("mod_id")}-neoforge-mc${property("minecraft_version")}"

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
}

// A "client" source set makes Stonecutter preprocess src/client here too, so the client @Mod and client
// mixins are built and folded into the jar.
val clientSourceSet = sourceSets.create("client")
clientSourceSet.compileClasspath += sourceSets["main"].compileClasspath + sourceSets["main"].output
clientSourceSet.runtimeClasspath += sourceSets["main"].runtimeClasspath + sourceSets["main"].output

neoForge {
    version = property("neoforge_version").toString()
    // MDG creates no run tasks unless declared. Share the root run/ dir (reuses its eula.txt).
    runs {
        create("client") {
            client()
            gameDirectory.set(rootProject.file("run"))
            // Dev convenience: -Pquickplay=<world> makes runClient join that singleplayer world directly.
            findProperty("quickplay")?.let { world ->
                programArguments.addAll("--quickPlaySingleplayer", world as String)
            }
        }
        create("server") { server(); gameDirectory.set(rootProject.file("run")) }
    }
    mods {
        create(property("mod_id") as String) {
            sourceSet(sourceSets["main"])
            sourceSet(clientSourceSet)
        }
    }
}

// Mixin configs are listed by hand in neoforge.mods.toml (no auto-inject). Applying the plugin
// also wires the manual "client" source set above into Stonecutter preprocessing.
fletchingTable {
    neoforge { applyMixinConfig = false }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.register<Copy>("collectJars") {
    group = "build"
    from(tasks.jar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
    dependsOn("build", rootProject.tasks.named("cleanCollectedJars"))
}

// ModDevGradle only jars `main`; fold the client source set (classes + processed client mixin config) in.
tasks.jar {
    from(clientSourceSet.output)
    from(rootProject.file("LICENSE")) {
        rename { "LICENSE_optimizedcushions" }
    }
}

// Resolve project properties outside the task lambda (the task delegate does not see them).
val modVersion = property("mod_version")
val supportedMc = property("supported_minecraft_version")
val neoforgeVersion = property("neoforge_version")
tasks.processResources {
    val expansions = mapOf(
        "version" to modVersion,
        "supported_minecraft_version" to supportedMc,
        "neoforge_version" to neoforgeVersion
    )
    inputs.properties(expansions)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expansions)
    }
}

// Stonecutter's generated sources must exist before ModDevGradle derives its artifacts.
tasks.named("createMinecraftArtifacts") {
    dependsOn(tasks.named("stonecutterGenerate"))
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
    modLoaders.add("neoforge")

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
