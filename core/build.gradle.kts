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
    implementation("net.momirealms:sparrow-expr:${rootProject.properties["sparrow_expression_version"]}")
    implementation("net.momirealms:sparrow-yaml:${rootProject.properties["sparrow_yaml_version"]}")
    // S3
    implementation("net.momirealms:craft-engine-s3:0.22")
    // Util
    compileOnly("net.momirealms:sparrow-util:${rootProject.properties["sparrow_util_version"]}")
    // Reflection
    compileOnly("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    compileOnly(files("${rootProject.rootDir}/libs/jni-internal-lookup-1.9.jar"))
    common(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    adventure(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
    netty(project, JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    archiveFileName = "craft-engine-core-${rootProject.properties["project_version"]}.jar"
}

publishing {
    publications {
        create<MavenPublication>("core") {
            groupId = "net.momirealms"
            artifactId = "craft-engine-core"
            version = rootProject.properties["project_version"].toString()
            from(components["shadow"])
            artifact(tasks["sourcesJar"])
            publication.applyCommonPom(this, "CraftEngine Core API")
        }
    }
}
