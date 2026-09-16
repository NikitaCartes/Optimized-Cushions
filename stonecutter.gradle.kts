plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
}

// Base = the node whose source most closely matches the upstream 26.3 mod (OptimizedCushions).
stonecutter active "26.2-fabric"

stonecutter parameters {
    // Loader flag constants from the node-name suffix: enables `//? if fabric`, `//? if neoforge`,
    // `//? if forge` guards in shared source.
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge", "forge")

    // ResourceLocation was renamed to Identifier in 1.21.11. Source is written with the pre-1.21.11
    // name; files opting in with `//~ resource_location` are converted forward on >=1.21.11 nodes.
    replacements.string(current.parsed >= "1.21.11", "resource_location") {
        replace("ResourceLocation", "Identifier")
        replace("location()", "identifier()")
    }
}

stonecutter.tasks {
    order("publishMods")
}

tasks.register<Delete>("cleanCollectedJars") {
    delete(layout.buildDirectory.dir("libs"))
}

publishMods {
    val githubToken = System.getenv("GITHUB_TOKEN") ?: ""
    val modVersion = findProperty("mod_version")?.toString()
        ?: file("stonecutter.properties.toml").readLines()
            .first { it.trim().startsWith("mod_version") }
            .substringAfter('=').trim().trim('"')

    dryRun = githubToken.isEmpty()
    version = modVersion
    displayName = modVersion
    changelog = rootProject.file("RELEASE_NOTE.md").readText()
    type = STABLE

    github {
        accessToken = githubToken
        repository = "NikitaCartes/Optimized-Cushions"
        commitish = "master"
        tagName = modVersion
        allowEmptyFiles = true
    }
}
