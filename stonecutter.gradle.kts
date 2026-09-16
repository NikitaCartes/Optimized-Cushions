plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    // Loader flag constants from the node-name suffix: enables `//? if fabric`,
    // `//? if neoforge` guards in shared source.
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge")
}
