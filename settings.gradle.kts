rootProject.name = "craft-engine"
include(":core")
include(":core:adventure")
include(":bukkit")
include(":bukkit:legacy")
include(":bukkit:compatibility")
include(":bukkit:compatibility:legacy")
include(":bukkit:loader")
include(":bukkit:proxy")
include(":bukkit:paper-loader")
include(":common-files")
pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.10"
    }
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
}