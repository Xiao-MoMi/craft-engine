repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    implementation(project(":bedrock"))
    compileOnly(libs.compat.geyser)
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
        archiveFileName = "craft-engine-geyser-extension-${project.version}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }

    assemble {
        dependsOn(shadowJar)
    }
}
