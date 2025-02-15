plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0-beta6"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    implementation(project(":shared"))
    remapper("net.fabricmc:tiny-remapper:0.10.4:fat")
    paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.21.4-R0.1-SNAPSHOT")
    compileOnly("space.vectrix.ignite:ignite-api:1.1.0")
    compileOnly("net.fabricmc:sponge-mixin:0.15.2+mixin.0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
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

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

artifacts {
    archives(tasks.shadowJar)
}

tasks {
    shadowJar {
        archiveClassifier = ""
        archiveFileName = "${rootProject.name}-bukkit-mod-${rootProject.properties["project_version"]}.jar"
        destinationDirectory.set(file("$rootDir/target"))
    }
}
