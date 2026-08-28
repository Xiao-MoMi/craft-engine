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
//    maven("https://hub.spigotmc.org/nexus/repository/snapshots/")
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
    adventure(project)
    // Anti Grief
    implementation(libs.anti.grief)
    // Reflection
    compileOnly(libs.sparrow.reflection)
    compileOnly(files("${rootProject.rootDir}/libs/jni-internal-lookup-${versionOf("jni-internal-lookup")}.jar"))
    // Util
    compileOnly(libs.sparrow.util)
    // NMS
    compileOnly(libs.nms.helper)
    // BStats
    compileOnly(libs.bstats.bukkit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        relocation.applyCommon(this)
        archiveClassifier = ""
        archiveFileName = "craft-engine-bukkit-${project.version}.jar"
    }
    compileJava {
        options.compilerArgs.addAll(
            listOf("-Xmaxerrs", "1000")
        )
    }
    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        create<MavenPublication>("bukkit") {
            artifactId = "craft-engine-bukkit"
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Bukkit API")
        }
    }
}
