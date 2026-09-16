plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.141"
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.22"
}

// Tag this node so [neoforge."<version>"] properties.toml entries resolve as bare property(...) lookups.
stonecutter {
    val (version, loader) = current.project.split('-', limit = 2)
    properties.tags(version, loader)
}

base.archivesName = "${property("mod_id")}-neoforge-mc${property("minecraft_version")}"

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://api.modrinth.com/maven")
}

// A "client" source set makes Stonecutter preprocess src/client here too, so the client @Mod and client
// mixins are built and folded into the jar.
val clientSourceSet = sourceSets.create("client")
clientSourceSet.compileClasspath += sourceSets["main"].compileClasspath + sourceSets["main"].output
clientSourceSet.runtimeClasspath += sourceSets["main"].runtimeClasspath + sourceSets["main"].output

neoForge {
    version = property("neoforge_version").toString()
    // Widen ModelPart.Polygon/Vertex (package-private on NeoForge's <1.21.11 jar) for the field-based
    // baker; no-op on >=1.21.11 where they're public records. See accesstransformer.cfg.
    accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
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

// j52j strips json5 comments from the .json5 mixin configs (Stonecutter preprocesses their per-version
// entries) into the .json that neoforge.mods.toml references. applyMixinConfig=false: they're listed by hand.
fletchingTable {
    neoforge { applyMixinConfig = false }
    j52j.register("main") { extension("json", "optimizedcushions.mixins.json5") }
    j52j.register("client") { extension("json", "optimizedcushions.client.mixins.json5") }
}

dependencies {
    // Dev-runtime test mods (not shipped); version IDs live in stonecutter.properties.toml.
    // fletchingTable.modrinth() clashes with the Fabric plugin variant across the multiloader build.
    runtimeOnly("maven.modrinth:cushions-backport:${property("cushionbackport_version_id")}")
    if (findProperty("with_sodium") == "true") {
        runtimeOnly("maven.modrinth:sodium:${property("sodium_version_id")}")
    }
}

val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">=26.1")) 25 else 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
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
}

// Resolve project properties outside the task lambda (see build.fabric-deobf.gradle.kts).
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
