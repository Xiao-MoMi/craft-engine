import net.momirealms.*

plugins {
    id("craft-engine-publish")
}

repositories {
    maven("https://jitpack.io/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.gtemc.net/releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    nbt(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
    netty(project)
    common(project)
    cloud(project)
    compression(project)
    adventure(project)
    implementation(libs.sparrow.expression)
    implementation(libs.sparrow.yaml)
    // S3
    implementation(libs.craft.engine.s3)
    // Util
    compileOnly(libs.sparrow.util)
    // Reflection
    compileOnly(libs.sparrow.reflection)
    compileOnly(files("${rootProject.rootDir}/libs/jni-internal-lookup-${versionOf("jni-internal-lookup")}.jar"))
    common(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    adventure(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    netty(project, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
    compression(project, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    relocation.applyCommon(this)
    val adventureBundle = project(":core:adventure").tasks.shadowJar
    dependsOn(adventureBundle)
    from({ zipTree(adventureBundle.get().archiveFile) })
    archiveClassifier = ""
    archiveFileName = "craft-engine-core-${project.version}.jar"
}

publishing {
    publications {
        create<MavenPublication>("core") {
            artifactId = "craft-engine-core"
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Core API")
        }
    }
}
