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
    implementation(libs.gson)
    implementation(libs.sparrow.reflection)
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
            "geyser_api_version" to libs.versions.geyser.get().substringBefore('-')
        )
        inputs.properties(properties)
        filesMatching("extension.yml") {
            expand(properties)
        }
    }

    shadowJar {
        relocate("com.google.gson", "net.momirealms.craftengine.bedrock.geyser.libs.gson")
        relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.bedrock.geyser.libs.reflection")
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
