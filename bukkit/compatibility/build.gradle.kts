repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // papi
    maven("https://maven.enginehub.org/repo/") // worldguard worldedit
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")  // slime world
    maven("https://mvn.lumine.io/repository/maven-public/") // model engine mythic mobs
    maven("https://repo.viaversion.com") // via
    maven("https://repo.skriptlang.org/releases/") // skript
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/") // quickshop
    maven("https://repo.opencollab.dev/main/") // geyser
}

dependencies {
    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
    compileOnly(project(":bukkit:compatibility:legacy"))
    compileOnly("net.momirealms:sparrow-nbt:${rootProject.properties["sparrow_nbt_version"]}")
    // NMS
    compileOnly("net.momirealms:craft-engine-nms-helper:${rootProject.properties["nms_helper_version"]}")
    // Platform
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")
    // Netty
    compileOnly("io.netty:netty-all:${rootProject.properties["netty_version"]}")
    // Placeholder
    compileOnly("me.clip:placeholderapi:${rootProject.properties["placeholder_api_version"]}")
    // SlimeWorld
    compileOnly("com.infernalsuite.asp:api:4.2.0-SNAPSHOT")
    // ModelEngine
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.8")
    // BetterModel
    compileOnly("io.github.toxicity188:bettermodel:1.15.2")
    compileOnly("com.mojang:authlib:${rootProject.properties["authlib_version"]}")
    // LuckPerms
    compileOnly("net.luckperms:api:5.4")
    // viaversion
    compileOnly("com.viaversion:viaversion-api:5.5.1")
    compileOnly("com.viaversion:viaversion-bukkit:5.5.1")
    // Skript
    compileOnly("com.github.SkriptLang:Skript:2.11.0")
    // FAWE
    compileOnly(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    // MythicMobs
    compileOnly("io.lumine:Mythic-Dist:5.9.0")
    // CustomNameplates
    compileOnly("net.momirealms:custom-nameplates:3.0.33")
    // WorldGuard
    compileOnly(files("${rootProject.rootDir}/libs/worldguard-bukkit-7.0.14-dist.jar"))
    // QuickShop
    compileOnly("com.ghostchu:quickshop-api:6.2.0.10")
    // Geyser
    compileOnly("org.geysermc.geyser:api:2.9.0-SNAPSHOT")
    // Floodgate
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    // ItemBridge
    compileOnly("cn.gtemc:itembridge:${rootProject.properties["itembridge_version"]}")
    // LevelerBridge
    compileOnly("cn.gtemc:levelerbridge:${rootProject.properties["levelerbridge_version"]}")
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