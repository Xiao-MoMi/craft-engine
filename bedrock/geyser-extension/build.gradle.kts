repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    implementation(project(":bedrock"))
    compileOnly(libs.compat.geyser)
    implementation(libs.gson)
    testImplementation(libs.compat.geyser)
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
