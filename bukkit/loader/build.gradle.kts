import net.momirealms.paperServer

plugins {
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
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
    implementation(project(":bukkit"))
    implementation(project(":bukkit:legacy"))
    implementation(project(":bukkit:compatibility"))
    implementation(project(":bukkit:compatibility:legacy"))
    implementation(project(":common-files"))

    // concurrentutil
    implementation(files("${rootProject.rootDir}/libs/concurrentutil-${rootProject.properties["concurrent_util_version"]}.jar"))

    implementation("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    implementation("net.momirealms:antigrieflib:${rootProject.properties["anti_grief_version"]}")
    implementation("net.momirealms:craft-engine-nms-helper:${rootProject.properties["nms_helper_version"]}")
    implementation("cn.gtemc:itembridge:${rootProject.properties["itembridge_version"]}")
    implementation("cn.gtemc:levelerbridge:${rootProject.properties["levelerbridge_version"]}")
    implementation(files("${rootProject.rootDir}/libs/jni-internal-lookup-1.9.jar"))
}

bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "net.momirealms.craftengine.bukkit.plugin.BukkitCraftEnginePlugin"
    version = rootProject.properties["project_version"] as String
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
        archiveFileName = "${rootProject.name}-bukkit-plugin-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}
