plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    // Loader flag constants from the node-name suffix: enables `//? if fabric`,
    // `//? if neoforge` guards in shared source.
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge")
}

stonecutter.tasks {
    order("publishMods")
}

tasks.register<Delete>("cleanCollectedJars") {
    delete(layout.buildDirectory.dir("libs"))
}

// One GitHub release for both loader jars; each node attaches its jar via `parent`.
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
