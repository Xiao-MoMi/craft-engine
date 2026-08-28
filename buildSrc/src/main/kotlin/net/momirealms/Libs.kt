package net.momirealms

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.getByType

private val Project.libraries: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.versionOf(alias: String): String = libraries
    .findVersion(alias)
    .orElseThrow { IllegalArgumentException("Unknown version catalog alias: $alias") }
    .requiredVersion

private fun DependencyHandlerScope.addBundle(
    project: Project,
    alias: String,
    configuration: String
) {
    add(
        configuration,
        project.libraries.findBundle(alias)
            .orElseThrow { IllegalArgumentException("Unknown dependency bundle: $alias") }
    )
}

private fun DependencyHandlerScope.addLibrary(
    project: Project,
    alias: String,
    configuration: String
) = add(
    configuration,
    project.libraries.findLibrary(alias)
        .orElseThrow { IllegalArgumentException("Unknown dependency alias: $alias") }
)

fun DependencyHandlerScope.nbt(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addBundle(project, "nbt", configuration)

fun DependencyHandlerScope.common(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) {
    addBundle(project, "common", configuration)
    add(
        configuration,
        project.files("${project.rootProject.rootDir}/libs/leafpile-${project.versionOf("leafpile")}.jar")
    )
}

fun DependencyHandlerScope.netty(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addBundle(project, "netty", configuration)

fun DependencyHandlerScope.compression(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addBundle(project, "compression", configuration)

fun DependencyHandlerScope.cloud(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addBundle(project, "cloud", configuration)

fun DependencyHandlerScope.paperServer(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addLibrary(project, "paper-api", configuration)

fun DependencyHandlerScope.asm(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) = addBundle(project, "asm", configuration)

fun DependencyHandlerScope.adventure(
    project: Project,
    configuration: String = JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
) {
    addBundle(project, "adventure", configuration)
    addLibrary(project, "adventure-gson", configuration).apply {
        (this as? ExternalModuleDependency)?.exclude("com.google.code.gson", "gson")
    }
    addLibrary(project, "sparrow-minimessage", configuration)
}
