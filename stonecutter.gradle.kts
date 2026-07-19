plugins {
    id("dev.kikugie.stonecutter")
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
