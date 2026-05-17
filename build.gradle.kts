import net.momirealms.PublishExtension
import net.momirealms.RelocationExtension
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("java")
}

subprojects {

    apply {
        plugin("java")
        plugin("java-library")
        plugin("com.gradleup.shadow")
        plugin("maven-publish")
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    extensions.create<RelocationExtension>("relocation")
    extensions.create<PublishExtension>("publication")

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        dependsOn(tasks.clean)
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
        disableAutoTargetJvm()
    }

    tasks.processResources {
        filteringCharset = "UTF-8"

        filesMatching(arrayListOf("craft-engine.properties")) {
            expand(
                rootProject.properties + mapOf(
                    "proxy_version" to getTimestamp(),
                    "git_version" to versionBanner(),
                    "builder" to builderName()
                )
            )
        }

        filesMatching(arrayListOf("commands.yml", "config.yml")) {
            expand(
                Pair("config_version", rootProject.properties["config_version"]!!)
            )
        }
    }
}

fun versionBanner(): String = project.providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
}.standardOutput.asText.map { it.trim() }.getOrElse("Unknown")

fun builderName(): String = providers.exec {
    commandLine("git", "config", "user.name")
}.standardOutput.asText.map { it.trim() }.getOrElse("Unknown")

fun getTimestamp(): String {
    return SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
}