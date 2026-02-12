plugins {
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.gtemc.net/releases/")
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":bukkit:legacy"))
    // Anti Grief
    compileOnly("net.momirealms:antigrieflib:${rootProject.properties["anti_grief_version"]}")
    // NBT
    compileOnly("net.momirealms:sparrow-nbt:${rootProject.properties["sparrow_nbt_version"]}")
    compileOnly("net.momirealms:sparrow-nbt-adventure:${rootProject.properties["sparrow_nbt_version"]}")
    compileOnly("net.momirealms:sparrow-nbt-codec:${rootProject.properties["sparrow_nbt_version"]}")
    compileOnly("net.momirealms:sparrow-nbt-legacy-codec:${rootProject.properties["sparrow_nbt_version"]}")
    // Util
    compileOnly("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    // NMS
    compileOnly("net.momirealms:craft-engine-nms-helper:${rootProject.properties["nms_helper_version"]}")
    // Platform
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")
    // OpenGL Math
    compileOnly("org.joml:joml:${rootProject.properties["joml_version"]}")
    // Gson
    compileOnly("com.google.code.gson:gson:${rootProject.properties["gson_version"]}")
    // Guava
    compileOnly("com.google.guava:guava:${rootProject.properties["guava_version"]}")
    // FastUtil
    compileOnly("it.unimi.dsi:fastutil:${rootProject.properties["fastutil_version"]}")
    // Netty
    compileOnly("io.netty:netty-all:${rootProject.properties["netty_version"]}")
    // ByteBuddy
    compileOnly("net.bytebuddy:byte-buddy:${rootProject.properties["byte_buddy_version"]}")
    compileOnly("net.bytebuddy:byte-buddy-agent:${rootProject.properties["byte_buddy_version"]}")
    // Command
    compileOnly("org.incendo:cloud-core:${rootProject.properties["cloud_core_version"]}")
    compileOnly("org.incendo:cloud-minecraft-extras:${rootProject.properties["cloud_minecraft_extras_version"]}")
    compileOnly("org.incendo:cloud-paper:${rootProject.properties["cloud_paper_version"]}")
    // YAML
    compileOnly(files("${rootProject.rootDir}/libs/boosted-yaml-${rootProject.properties["boosted_yaml_version"]}.jar"))
    // Adventure
    compileOnly("net.kyori:adventure-api:${rootProject.properties["adventure_bundle_version"]}")
    compileOnly("net.kyori:adventure-text-minimessage:${rootProject.properties["adventure_bundle_version"]}")
    compileOnly("net.kyori:adventure-text-serializer-gson:${rootProject.properties["adventure_bundle_version"]}") {
        exclude("com.google.code.gson", "gson")
    }
    // Data Fixer Upper
    compileOnly("com.mojang:datafixerupper:${rootProject.properties["datafixerupper_version"]}")
    // BStats
    compileOnly("org.bstats:bstats-bukkit:${rootProject.properties["bstats_version"]}")
    // Aho-Corasick java implementation
    compileOnly("org.ahocorasick:ahocorasick:${rootProject.properties["ahocorasick_version"]}")
    // authlib
    compileOnly("com.mojang:authlib:${rootProject.properties["authlib_version"]}")
    // concurrentutil
    compileOnly("ca.spottedleaf:concurrentutil:${rootProject.properties["concurrent_util_version"]}")
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

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        archiveClassifier = ""
        archiveFileName = "craft-engine-bukkit-${rootProject.properties["project_version"]}.jar"
        relocate("net.kyori", "net.momirealms.craftengine.libraries")
        relocate("net.momirealms.sparrow.nbt", "net.momirealms.craftengine.libraries.nbt")
        relocate("net.momirealms.antigrieflib", "net.momirealms.craftengine.libraries.antigrieflib")
        relocate("cn.gtemc.itembridge", "net.momirealms.craftengine.libraries.itembridge")
        relocate("cn.gtemc.levelerbridge", "net.momirealms.craftengine.libraries.levelerbridge")
        relocate("org.incendo", "net.momirealms.craftengine.libraries")
        relocate("dev.dejvokep", "net.momirealms.craftengine.libraries")
        relocate("org.bstats", "net.momirealms.craftengine.libraries.bstats")
        relocate("com.github.benmanes.caffeine", "net.momirealms.craftengine.libraries.caffeine")
        relocate("net.bytebuddy", "net.momirealms.craftengine.libraries.bytebuddy")
        relocate("org.yaml.snakeyaml", "net.momirealms.craftengine.libraries.snakeyaml")
        relocate("org.ahocorasick", "net.momirealms.craftengine.libraries.ahocorasick")
        relocate("com.ezylang.evalex", "net.momirealms.craftengine.libraries.evalex")
        relocate("com.google.common.jimfs", "net.momirealms.craftengine.libraries.jimfs")
        relocate("org.apache.commons", "net.momirealms.craftengine.libraries.commons")
        relocate("io.leangen.geantyref", "net.momirealms.craftengine.libraries.geantyref")
        relocate("io.netty.handler.codec.http", "net.momirealms.craftengine.libraries.netty.handler.codec.http")
        relocate("io.netty.handler.codec.rtsp", "net.momirealms.craftengine.libraries.netty.handler.codec.rtsp")
        relocate("io.netty.handler.codec.spdy", "net.momirealms.craftengine.libraries.netty.handler.codec.spdy")
        relocate("io.netty.handler.codec.http2", "net.momirealms.craftengine.libraries.netty.handler.codec.http2")
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
        create<MavenPublication>("bukkit") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit"
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
        create<MavenPublication>("bukkitSnapshot") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit"
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
    }
}

tasks.register("publishRelease") {
    group = "publishing"
    description = "Publishes to the release repository"
    dependsOn("publishBukkitPublicationToReleasesRepository")
}

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publishes to the snapshot repository"
    dependsOn("publishBukkitSnapshotPublicationToSnapshotRepository")
}