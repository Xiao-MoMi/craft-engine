import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("com.gradleup.shadow") version "9.0.0-beta13"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

repositories {
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    mavenCentral()
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")

    implementation(project(":core"))
    implementation(project(":bukkit"))
    implementation(project(":bukkit:legacy"))
    implementation(project(":bukkit:compatibility"))
    implementation(project(":bukkit:compatibility:legacy"))
    implementation(project(":common-files"))

    implementation("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    implementation("net.momirealms:antigrieflib:${rootProject.properties["anti_grief_version"]}")
    implementation("net.momirealms:craft-engine-nms-helper-mojmap:${rootProject.properties["nms_helper_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    dependsOn(tasks.clean)
}

paper {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "net.momirealms.craftengine.bukkit.plugin.PaperCraftEnginePlugin"
    bootstrapper = "net.momirealms.craftengine.bukkit.plugin.PaperCraftEngineBootstrap"
    version = rootProject.properties["project_version"] as String
    name = "CraftEngine"
    apiVersion = "1.20"
    authors = listOf("XiaoMoMi")
    contributors = listOf("jhqwqmc", "iqtesterrr", "WhiteProject1", "Catnies", "xiaozhangup", "TamashiiMon")
    foliaSupported = true
    serverDependencies {
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("WorldEdit") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("FastAsyncWorldEdit") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = false
        }
        register("Skript") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("LuckPerms") { required = false }
        register("ViaVersion") { required = false }

        // external models
        register("ModelEngine") { required = false }
        register("BetterModel") { required = false }

        // external items
        register("NeigeItems") { required = false }
        register("MMOItems") { required = false }
        register("MythicMobs") { required = false }
        register("CustomFishing") { required = false }

        // leveler
        register("AuraSkills") { required = false }
        register("AureliumSkills") { required = false }
        register("McMMO") { required = false }
        register("MMOCore") { required = false }
        register("Jobs") { required = false }
        register("EcoSkills") { required = false }
        register("EcoJobs") { required = false }

        // anti grief lib
        register("Dominion") { required = false }
        register("WorldGuard") { required = false }
        register("Kingdoms") { required = false }
        register("Lands") { required = false }
        register("IridiumSkyblock") { required = false }
        register("CrashClaim") { required = false }
        register("GriefDefender") { required = false }
        register("HuskClaims") { required = false }
        register("BentoBox") { required = false }
        register("HuskTowns") { required = false }
        register("PlotSquared") { required = false }
        register("Residence") { required = false }
        register("SuperiorSkyblock2") { required = false }
        register("Towny") { required = false }
        register("FabledSkyBlock") { required = false }
        register("GriefPrevention") { required = false }
        register("RedProtect") { required = false }
        register("Landlord") { required = false }
        register("uSkyBlock") { required = false }
        register("XClaim") { required = false }
        register("UltimateClaims") { required = false }
        register("UltimateClans") { required = false }
        register("PreciousStones") { required = false }
        register("hClaims") { required = false }
        register("Factions") { required = false }
    }
}

artifacts {
    archives(tasks.shadowJar)
}

tasks {
    shadowJar {
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
        archiveFileName = "${rootProject.name}-paper-plugin-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
        relocate("net.kyori", "net.momirealms.craftengine.libraries")
        relocate("net.momirealms.sparrow.nbt", "net.momirealms.craftengine.libraries.nbt")
        relocate("net.momirealms.antigrieflib", "net.momirealms.craftengine.libraries.antigrieflib")
        relocate("org.incendo", "net.momirealms.craftengine.libraries")
        relocate("dev.dejvokep", "net.momirealms.craftengine.libraries")
        relocate("org.bstats", "net.momirealms.craftengine.libraries.bstats")
        relocate("com.github.benmanes.caffeine", "net.momirealms.craftengine.libraries.caffeine")
        relocate("com.ezylang.evalex", "net.momirealms.craftengine.libraries.evalex")
        relocate("net.bytebuddy", "net.momirealms.craftengine.libraries.bytebuddy")
        relocate("org.yaml.snakeyaml", "net.momirealms.craftengine.libraries.snakeyaml")
        relocate("org.ahocorasick", "net.momirealms.craftengine.libraries.ahocorasick")
        relocate("net.jpountz", "net.momirealms.craftengine.libraries.jpountz")
        relocate("software.amazon.awssdk", "net.momirealms.craftengine.libraries.awssdk")
        relocate("software.amazon.eventstream", "net.momirealms.craftengine.libraries.eventstream")
        relocate("com.google.common.jimfs", "net.momirealms.craftengine.libraries.jimfs")
        relocate("org.apache.commons", "net.momirealms.craftengine.libraries.commons")
    }
}
