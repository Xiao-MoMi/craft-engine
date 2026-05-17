import net.momirealms.*

plugins {
    id("craft-engine-publish")
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
    compileOnly(project(":bukkit:proxy"))

    common(project)
    nbt(project)
    netty(project)
    asm(project)
    paperServer(project)
    cloud(project)
    // Anti Grief
    compileOnly("net.momirealms:antigrieflib:${rootProject.properties["anti_grief_version"]}")
    // Reflection
    compileOnly("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    compileOnly(files("${rootProject.rootDir}/libs/jni-internal-lookup-1.9.jar"))
    // Util
    compileOnly("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    // NMS
    compileOnly("net.momirealms:craft-engine-nms-helper:${rootProject.properties["nms_helper_version"]}")
    // BStats
    compileOnly("org.bstats:bstats-bukkit:${rootProject.properties["bstats_version"]}")
}

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        relocation.applyCommon(this)
        archiveClassifier = ""
        archiveFileName = "craft-engine-bukkit-${rootProject.properties["project_version"]}.jar"
    }
}

publishing {
    publications {
        create<MavenPublication>("bukkit") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-bukkit"
            version = rootProject.properties["project_version"].toString()
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Bukkit API")
        }
    }
}