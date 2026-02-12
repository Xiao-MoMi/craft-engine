import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

val adventureLibraries: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileOnly.get().extendsFrom(adventureLibraries)
}

repositories {
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
}

dependencies {
    // JOML
    compileOnly("org.joml:joml:${rootProject.properties["joml_version"]}")
    // YAML
    compileOnly(files("${rootProject.rootDir}/libs/boosted-yaml-${rootProject.properties["boosted_yaml_version"]}.jar"))
    compileOnly("org.yaml:snakeyaml:${rootProject.properties["snake_yaml_version"]}")
    // NBT
    implementation("net.momirealms:sparrow-nbt:${rootProject.properties["sparrow_nbt_version"]}")
    implementation("net.momirealms:sparrow-nbt-adventure:${rootProject.properties["sparrow_nbt_version"]}")
    implementation("net.momirealms:sparrow-nbt-codec:${rootProject.properties["sparrow_nbt_version"]}")
    implementation("net.momirealms:sparrow-nbt-legacy-codec:${rootProject.properties["sparrow_nbt_version"]}")
    // S3
    implementation("net.momirealms:craft-engine-s3:0.14")
    // Util
    compileOnly("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    // Adventure
    adventureLibraries("net.kyori:adventure-api:${rootProject.properties["adventure_bundle_version"]}")
    adventureLibraries("net.kyori:adventure-text-minimessage:${rootProject.properties["adventure_bundle_version"]}")
    adventureLibraries("net.kyori:adventure-text-serializer-gson:${rootProject.properties["adventure_bundle_version"]}") {
        exclude("com.google.code.gson", "gson")
    }
    adventureLibraries("net.kyori:adventure-text-serializer-legacy:${rootProject.properties["adventure_bundle_version"]}")
    adventureLibraries("net.kyori:adventure-text-serializer-json-legacy-impl:${rootProject.properties["adventure_bundle_version"]}")
    // Command
    compileOnly("org.incendo:cloud-core:${rootProject.properties["cloud_core_version"]}")
    compileOnly("org.incendo:cloud-minecraft-extras:${rootProject.properties["cloud_minecraft_extras_version"]}")
    // FastUtil
    compileOnly("it.unimi.dsi:fastutil:${rootProject.properties["fastutil_version"]}")
    // Gson
    compileOnly("com.google.code.gson:gson:${rootProject.properties["gson_version"]}")
    // Guava
    compileOnly("com.google.guava:guava:${rootProject.properties["guava_version"]}")
    // Logger
    compileOnly("org.slf4j:slf4j-api:${rootProject.properties["slf4j_version"]}")
    compileOnly("org.apache.logging.log4j:log4j-core:${rootProject.properties["log4j_version"]}")
    // Netty
    compileOnly("io.netty:netty-all:${rootProject.properties["netty_version"]}")
    compileOnly("io.netty:netty-codec-http:${rootProject.properties["netty_version"]}")
    // Cache
    compileOnly("com.github.ben-manes.caffeine:caffeine:${rootProject.properties["caffeine_version"]}")
    // Compression
    compileOnly("com.github.luben:zstd-jni:${rootProject.properties["zstd_version"]}")
    compileOnly("at.yawk.lz4:lz4-java:${rootProject.properties["lz4_version"]}")
    // Commons IO
    compileOnly("commons-io:commons-io:${rootProject.properties["commons_io_version"]}")
    // Data Fixer Upper
    compileOnly("com.mojang:datafixerupper:${rootProject.properties["datafixerupper_version"]}")
    // Aho-Corasick java implementation
    compileOnly("org.ahocorasick:ahocorasick:${rootProject.properties["ahocorasick_version"]}")
    // EvalEx
    compileOnly("com.ezylang:EvalEx:${rootProject.properties["evalex_version"]}")
    // Jimfs
    compileOnly("com.google.jimfs:jimfs:${rootProject.properties["jimfs_version"]}")
    // Brigadier
    compileOnly("com.mojang:brigadier:${rootProject.properties["mojang_brigadier_version"]}")
    // authlib
    compileOnly("com.mojang:authlib:${rootProject.properties["authlib_version"]}")
    // concurrentutil
    compileOnly("ca.spottedleaf:concurrentutil:${rootProject.properties["concurrent_util_version"]}")
    // bucket4j
    compileOnly("com.bucket4j:bucket4j_jdk17-core:${rootProject.properties["bucket4j_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

tasks.register("adventureBundle", ShadowJar::class) {
    group = "build"
    archiveBaseName.set("adventure-bundle")
    archiveClassifier.set("")
    configurations = listOf(adventureLibraries)
    relocate("net.kyori", "net.momirealms.craftengine.libraries")
}

tasks {
    shadowJar {
        archiveClassifier = ""
        archiveFileName = "craft-engine-core-${rootProject.properties["project_version"]}.jar"
        relocate("net.kyori", "net.momirealms.craftengine.libraries")
        relocate("dev.dejvokep", "net.momirealms.craftengine.libraries")
        relocate("org.yaml.snakeyaml", "net.momirealms.craftengine.libraries.snakeyaml")
        relocate("org.ahocorasick", "net.momirealms.craftengine.libraries.ahocorasick")
        relocate("net.momirealms.sparrow.nbt", "net.momirealms.craftengine.libraries.nbt")
        relocate("net.jpountz", "net.momirealms.craftengine.libraries.jpountz") // lz4
        relocate("software.amazon.awssdk", "net.momirealms.craftengine.libraries.awssdk") // awssdk
        relocate("software.amazon.eventstream", "net.momirealms.craftengine.libraries.eventstream") // awssdk
        relocate("com.ezylang.evalex", "net.momirealms.craftengine.libraries.evalex")
        relocate("com.google.common.jimfs", "net.momirealms.craftengine.libraries.jimfs")
        relocate("org.apache.commons", "net.momirealms.craftengine.libraries.commons")
        relocate("io.leangen.geantyref", "net.momirealms.craftengine.libraries.geantyref")
        relocate("io.netty.handler.codec.http", "net.momirealms.craftengine.libraries.netty.handler.codec.http")
        relocate("io.netty.handler.codec.rtsp", "net.momirealms.craftengine.libraries.netty.handler.codec.rtsp")
        relocate("io.netty.handler.codec.spdy", "net.momirealms.craftengine.libraries.netty.handler.codec.spdy")
        relocate("io.netty.handler.codec.http2", "net.momirealms.craftengine.libraries.netty.handler.codec.http2")
        relocate("io.github.bucket4j", "net.momirealms.craftengine.libraries.bucket4j") // bucket4j
        relocate("cn.gtemc.itembridge", "net.momirealms.craftengine.libraries.itembridge")
        relocate("cn.gtemc.levelerbridge", "net.momirealms.craftengine.libraries.levelerbridge")
    }
}

publishing {
    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.momirealms.net/releases")
            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
        maven {
            name = "snapshot"
            url = uri("https://repo.momirealms.net/snapshots")
            credentials(PasswordCredentials::class) {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("core") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-core"
            version = rootProject.properties["project_version"].toString()
            artifact(tasks["sourcesJar"])
            from(components["shadow"])
            pom {
                name = "CraftEngine API"
                url = "https://github.com/Xiao-MoMi/craft-engine"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                        distribution = "repo"
                    }
                }
            }
        }
        create<MavenPublication>("coreSnapshot") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-core"
            version = "${rootProject.properties["project_version"]}-SNAPSHOT"
            artifact(tasks["sourcesJar"])
            from(components["shadow"])
            pom {
                name = "CraftEngine API"
                url = "https://github.com/Xiao-MoMi/craft-engine"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                        distribution = "repo"
                    }
                }
            }
        }
        create<MavenPublication>("adventure") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-adventure"
            version = rootProject.properties["project_version"].toString()
            artifact(tasks.named("adventureBundle"))
            pom {
                name = "CraftEngine API"
                url = "https://github.com/Xiao-MoMi/craft-engine"
            }
        }
        create<MavenPublication>("adventureSnapshot") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-adventure"
            version = "${rootProject.properties["project_version"]}-SNAPSHOT"
            artifact(tasks.named("adventureBundle"))
            pom {
                name = "CraftEngine API"
                url = "https://github.com/Xiao-MoMi/craft-engine"
            }
        }
    }
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes to the release repository"
    dependsOn("publishCorePublicationToReleasesRepository", "publishAdventurePublicationToReleasesRepository")
}

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publishes to the snapshot repository"
    dependsOn("publishCoreSnapshotPublicationToSnapshotRepository", "publishAdventureSnapshotPublicationToSnapshotRepository")
}