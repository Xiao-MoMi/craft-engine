import net.momirealms.adventure

repositories {
    mavenCentral()
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    adventure(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
}

configurations.implementation {
    exclude(mapOf("group" to "org.jspecify", "module" to "jspecify"))
}

tasks.shadowJar {
    archiveBaseName.set("adventure-bundle")
    archiveClassifier = ""
    relocate("net.kyori", "net.momirealms.craftengine.libraries")
    relocate("net.momirealms.sparrow.message", "net.momirealms.craftengine.libraries.message")
}
