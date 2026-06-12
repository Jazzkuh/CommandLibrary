plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "CommandLibrary"

include("common", "spigot", "velocity", "minestom", "jda")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.purpurmc.org/snapshots")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.velocitypowered.com/snapshots/")
    }
}
