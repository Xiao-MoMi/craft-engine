abstract class GenerateTranslationIndexTask : DefaultTask() {
    @get:InputDirectory
    abstract val translationsDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val files = translationsDirectory.get().asFile.listFiles()
            .orEmpty().filter { it.isFile && it.extension == "yml" }.map { it.name }.sorted()
        val index = outputDirectory.file("translations/_index.json").get().asFile
        index.parentFile.mkdirs()
        val names = files.joinToString(", ") { "\"$it\"" }
        index.writeText("{\"directory\": [], \"file\": [$names]}")
    }
}

val generateTranslationIndex = tasks.register<GenerateTranslationIndexTask>("generateTranslationIndex") {
    translationsDirectory.set(layout.projectDirectory.dir("src/main/resources/translations"))
    outputDirectory.set(layout.buildDirectory.dir("generated/translation-index"))
}

sourceSets.main {
    resources.srcDir(generateTranslationIndex.map { it.outputDirectory })
}

repositories {
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.momirealms.net/releases/")
}

configurations.configureEach {
    exclude(group = "org.geysermc.mcprotocollib", module = "text-serializer-nbt")
}

dependencies {
    implementation(project(":bedrock"))
    compileOnly(libs.compat.geyser)
    compileOnly("org.geysermc.geyser:core:${libs.versions.geyser.get()}")
    implementation(libs.sparrow.yaml)
    implementation(libs.gson)
    implementation(libs.sparrow.reflection)
    implementation(libs.byte.buddy)
    testImplementation(libs.compat.geyser)
    testImplementation("org.geysermc.geyser:core:${libs.versions.geyser.get()}")
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        val properties = mapOf(
            "version" to project.version.toString(),
            "geyser_api_version" to libs.versions.geyser.get().substringBefore('-'),
            "config_version" to providers.gradleProperty("config_version").get(),
            "lang_version" to providers.gradleProperty("lang_version").get()
        )
        inputs.properties(properties)
        filesMatching("extension.yml") {
            expand(properties)
        }
        filesMatching("config.yml") {
            expand("config_version" to properties.getValue("config_version"))
        }
        filesMatching("craft-engine-geyser.properties") {
            expand(
                "config_version" to properties.getValue("config_version"),
                "lang_version" to properties.getValue("lang_version")
            )
        }
    }

    shadowJar {
        dependencies {
            exclude(dependency("com.google.errorprone:error_prone_annotations:.*"))
        }
        relocate("net.momirealms.sparrow.yaml", "net.momirealms.craftengine.bedrock.geyser.libs.yaml")
        relocate("com.google.gson", "net.momirealms.craftengine.bedrock.geyser.libs.gson")
        relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.bedrock.geyser.libs.reflection")
        relocate("net.bytebuddy", "net.momirealms.craftengine.bedrock.geyser.libs.bytebuddy")
        archiveFileName = "craft-engine-geyser-extension-${project.version}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
