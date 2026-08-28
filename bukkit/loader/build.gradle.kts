import net.momirealms.paperServer
import net.momirealms.versionOf

plugins {
    alias(libs.plugins.bukkit.yml)
}

repositories {
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    mavenCentral()
}

dependencies {
    // Platform
    paperServer(project)

    implementation(project(":core"))
    implementation(project(":bukkit")) {
        exclude(group = "net.momirealms", module = "antigrieflib")
    }
    implementation(project(":bukkit:legacy"))
    implementation(project(":bukkit:compatibility"))
    implementation(project(":bukkit:compatibility:legacy"))
    implementation(project(":common-files"))

    // leafpile
    implementation(files("${rootProject.rootDir}/libs/leafpile-${versionOf("leafpile")}.jar"))

    implementation(libs.sparrow.minimessage)
    implementation(libs.sparrow.util)
    implementation(libs.nms.helper)
    implementation(libs.itembridge)
    implementation(libs.levelerbridge)
    implementation(files("${rootProject.rootDir}/libs/jni-internal-lookup-${versionOf("jni-internal-lookup")}.jar"))
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "net.momirealms.craftengine.bukkit.plugin.BukkitCraftEnginePlugin"
    version = project.version.toString()
    name = "CraftEngine"
    apiVersion = "1.20"
    authors = listOf("XiaoMoMi")
    contributors = listOf("https://github.com/Xiao-MoMi/craft-engine/graphs/contributors")
    softDepend = listOf("WorldEdit", "FastAsyncWorldEdit")
    foliaSupported = true
}

artifacts {
    implementation(tasks.shadowJar)
}

tasks {
    shadowJar {
        relocation.applyCommon(this)
        from(project(":bukkit:proxy").tasks.shadowJar.flatMap { it.archiveFile })
        archiveFileName = "${rootProject.name}-bukkit-plugin-${project.version}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}
