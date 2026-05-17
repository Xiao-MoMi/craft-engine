import net.momirealms.common
import net.momirealms.netty
import net.momirealms.paperServer

plugins {
    id("craft-engine-publish")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    // Platform
    paperServer(project)
    common(project)
    netty(project)
    implementation("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "proxy.jarinjar"
    relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.libraries.reflection")
}

artifacts {
    implementation(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("bukkitProxy") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit-proxy"
            version = rootProject.properties["project_version"].toString()
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Bukkit Proxy")
        }
    }
}