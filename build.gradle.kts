import net.momirealms.PublishExtension
import net.momirealms.RelocationExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
}

group = providers.gradleProperty("project_group").get()
version = providers.gradleProperty("project_version").get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin("java-library")
        plugin("com.gradleup.shadow")
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    extensions.create<RelocationExtension>("relocation")
    extensions.create<PublishExtension>("publication")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
        withSourcesJar()
        disableAutoTargetJvm()
    }
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val embeddedVersionAliases = mapOf(
    "asm_version" to "asm",
    "jar_relocator_version" to "jar-relocator",
    "cloud_core_version" to "cloud-core",
    "cloud_platform_version" to "cloud-platform",
    "bstats_version" to "bstats",
    "geantyref_version" to "geantyref",
    "gson_version" to "gson",
    "caffeine_version" to "caffeine",
    "slf4j_version" to "slf4j",
    "zstd_version" to "zstd",
    "commons_io_version" to "commons-io",
    "commons_lang3_version" to "commons-lang3",
    "byte_buddy_version" to "byte-buddy",
    "snake_yaml_version" to "snakeyaml",
    "option_version" to "option",
    "adventure_bundle_version" to "adventure",
    "netty_version" to "netty",
    "ahocorasick_version" to "ahocorasick",
    "lz4_version" to "lz4",
    "reactive_streams_version" to "reactive-streams",
    "amazon_awssdk_version" to "aws-sdk",
    "amazon_awssdk_eventstream_version" to "aws-eventstream",
    "jimfs_version" to "jimfs",
    "bucket4j_version" to "bucket4j",
    "graaljs_version" to "graaljs",
    "nashorn_version" to "nashorn"
)
val embeddedVersions = embeddedVersionAliases.mapValues { (_, alias) ->
    versionCatalog.findVersion(alias)
        .orElseThrow { IllegalArgumentException("Unknown embedded version alias: $alias") }
        .requiredVersion
}
val resourceVersions = mapOf(
    "config_version" to providers.gradleProperty("config_version").get(),
    "lang_version" to providers.gradleProperty("lang_version").get(),
    "latest_supported_version" to providers.gradleProperty("latest_supported_version").get()
)
val buildMetadata = mapOf(
    "proxy_version" to buildTimestamp(),
    "git_version" to versionBanner(),
    "builder" to builderName()
)

project(":common-files") {
    tasks.named<ProcessResources>("processResources") {
        filteringCharset = "UTF-8"

        val pluginProperties = resourceVersions + embeddedVersions + buildMetadata
        inputs.properties(pluginProperties)

        filesMatching("craft-engine.properties") {
            expand(pluginProperties)
        }
        filesMatching(listOf("commands.yml", "config.yml")) {
            expand("config_version" to resourceVersions.getValue("config_version"))
        }
    }
}

fun versionBanner(): String = providers.exec {
    commandLine("git", "rev-parse", "--short=8", "HEAD")
}.standardOutput.asText.map(String::trim).getOrElse("Unknown")

fun builderName(): String = providers.exec {
    commandLine("git", "config", "user.name")
}.standardOutput.asText.map(String::trim).getOrElse("Unknown")

fun buildTimestamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
