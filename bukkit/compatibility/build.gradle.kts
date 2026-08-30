import net.momirealms.*

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
//    maven("https://maven.citizensnpcs.co/repo/") // denizen
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/") // quickshop
    maven("https://repo.opencollab.dev/main/") // geyser
    maven("https://maven.playpro.com/") // coreprotect
}

dependencies {
    paperServer(project)
    nbt(project)
    netty(project)
    adventure(project)

    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
    compileOnly(project(":bukkit:proxy"))
    compileOnly(project(":bukkit:compatibility:legacy"))
    compileOnly(files("${rootProject.rootDir}/libs/leafpile-${versionOf("leafpile")}.jar"))

    // Reflection
    compileOnly(libs.sparrow.reflection)
    // NMS
    compileOnly(libs.nms.helper)
    // Placeholder
    compileOnly(libs.placeholder.api)
    // SlimeWorld
    compileOnly(libs.compat.asp)
    // ModelEngine
    compileOnly(libs.compat.modelengine)
    // BetterModel
    compileOnly(libs.compat.bettermodel)
    compileOnly(libs.authlib)
    // LuckPerms
    compileOnly(libs.compat.luckperms)
    // viaversion
    compileOnly(libs.compat.viaversion.api)
    compileOnly(libs.compat.viaversion.bukkit)
    // Skript
    compileOnly(libs.compat.skript)
    // Denizen
//    compileOnly(libs.compat.denizen)
    compileOnly(files("${rootProject.rootDir}/libs/denizen-${versionOf("denizen")}.jar"))
    // FAWE
    compileOnly(platform(libs.compat.fawe.bom))
    compileOnly(libs.compat.fawe.core)
    compileOnly(libs.compat.fawe.bukkit) { isTransitive = false }
    // MythicMobs
    compileOnly(libs.compat.mythicmobs)
    // CustomNameplates
    compileOnly(libs.compat.custom.nameplates)
    // Axiom
    compileOnly(files("${rootProject.rootDir}/libs/AxiomPaperPlugin-${versionOf("axiom")}.jar"))
    // WorldGuard
    compileOnly(files("${rootProject.rootDir}/libs/worldguard-bukkit-${versionOf("worldguard")}-dist.jar"))
    // QuickShop
    compileOnly(libs.compat.quickshop)
    // Geyser
    compileOnly(libs.compat.geyser)
    // Floodgate
    compileOnly(libs.compat.floodgate)
    // Vault
    compileOnly(libs.compat.vault)
    // ItemBridge
    compileOnly(libs.itembridge)
    // LevelerBridge
    compileOnly(libs.levelerbridge)
    // CoreProtect
    compileOnly(libs.compat.coreprotect)

    testImplementation(project(":core"))
    testImplementation(project(":bukkit"))
    testImplementation(libs.compat.bettermodel)
    nbt(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    common(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    adventure(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
