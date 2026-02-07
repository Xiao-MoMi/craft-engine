plugins {
    id("com.gradleup.shadow") version "9.3.0"
    id("maven-publish")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    mavenCentral()
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:${rootProject.properties["netty_version"]}")
    compileOnly("org.ow2.asm:asm:${rootProject.properties["asm_version"]}")
    implementation("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    compileOnly("com.mojang:datafixerupper:${rootProject.properties["datafixerupper_version"]}")
    compileOnly("com.mojang:authlib:${rootProject.properties["authlib_version"]}")
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "proxy.jarinjar"
    relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.libraries.reflection")
}

artifacts {
    implementation(tasks.shadowJar)
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
        create<MavenPublication>("bukkitProxy") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit-proxy"
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
        create<MavenPublication>("bukkitProxySnapshot") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit-proxy"
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
    dependsOn("publishBukkitProxyPublicationToReleasesRepository")
}

tasks.register("publishSnapshot") {
    group = "publishing"
    description = "Publishes to the snapshot repository"
    dependsOn("publishBukkitProxySnapshotPublicationToSnapshotRepository")
}