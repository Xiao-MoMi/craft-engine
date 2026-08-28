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
    implementation(libs.sparrow.reflection)
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "proxy.jarinjar"
    relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.libraries.reflection")
    relocate("net.kyori", "net.momirealms.craftengine.libraries")
}

artifacts {
    implementation(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("bukkitProxy") {
            artifactId = "craft-engine-bukkit-proxy"
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Bukkit Proxy")
        }
    }
}
