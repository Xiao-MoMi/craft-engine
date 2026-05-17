import net.momirealms.adventure

plugins {
    id("craft-engine-publish")
}

dependencies {
    adventure(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
}

tasks.shadowJar {
    archiveBaseName.set("adventure-bundle")
    archiveClassifier = ""
    relocate("net.kyori", "net.momirealms.craftengine.libraries")
}

publishing {
    publications {
        create<MavenPublication>("adventure") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-adventure"
            version = rootProject.properties["project_version"].toString()
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Adventure API")
        }
    }
}