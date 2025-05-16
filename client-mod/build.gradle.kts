plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

version = property("project_version")!!
group = property("project_group")!!
val project_version: String by project
val latest_minecraft_version: String by project
val loader_version: String by project
var modmenu_version = property("modmenu_version")
var cloth_version = property("cloth_version")

base {
    archivesName.set("craft-engine-fabric-mod")
}

sourceSets {
    create("client") {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
    main {
        compileClasspath += sourceSets["client"].output
        runtimeClasspath += sourceSets["client"].output
        output.dir(sourceSets["client"].output)
    }
}

tasks.shadowJar {
    relocate("org.yaml", "net.momirealms.craftengine.libraries.org.yaml")
    configurations = listOf(project.configurations.getByName("shadow"))
    archiveFileName.set("${base.archivesName.get()}-${project.version}-shadow.jar")
    from(sourceSets.main.get().output)
    from(sourceSets["client"].output)
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)

    destinationDirectory.set(file("$rootDir/target"))
    archiveFileName.set("${base.archivesName.get()}-${project.version}+mc${rootProject.properties["latest_minecraft_version"]}.jar")
}

loom {
    mods {
        create("craft-engine-fabric-mod") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("latest_minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_version")}")
    modApi("com.terraformersmc:modmenu:${property("modmenu_version")}")
    add("shadow", "org.yaml:snakeyaml:2.4")
}

tasks.processResources {
    inputs.property("version", project_version)
    inputs.property("minecraft_version", latest_minecraft_version)
    inputs.property("loader_version", loader_version)

    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project_version,
            "minecraft_version" to latest_minecraft_version,
            "loader_version" to loader_version,
            "modmenu_version" to modmenu_version,
            "cloth_version" to cloth_version
        )
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }
    withSourcesJar()
}

tasks.build {
    dependsOn(tasks.clean)
    dependsOn(tasks.shadowJar)
}