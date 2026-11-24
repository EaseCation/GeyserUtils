plugins {
    // Downloads the Java toolchains (17/21) automatically when they are not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "GeyserUtils"

// Project names match the published artifactIds so `--include-build ../GeyserUtils` substitutes them.
for (module in listOf("common", "geyser", "spigot", "velocity")) {
    include("geyserutils-$module")
    project(":geyserutils-$module").projectDir = file(module)
}
