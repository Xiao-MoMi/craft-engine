plugins {
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    mavenCentral()
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:${rootProject.properties["paper_version"]}-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:${rootProject.properties["netty_version"]}")
    compileOnly("org.ow2.asm:asm:${rootProject.properties["asm_version"]}")
    implementation("net.momirealms:sparrow-reflection:${rootProject.properties["sparrow_reflection_version"]}")
    compileOnly("com.mojang:datafixerupper:${rootProject.properties["datafixerupper_version"]}")
    compileOnly("com.mojang:authlib:${rootProject.properties["authlib_version"]}")
}

tasks.shadowJar {
    archiveFileName = "proxy.jarinjar"
    relocate("net.momirealms.sparrow.reflection", "net.momirealms.craftengine.libraries.reflection")
}

artifacts {
    implementation(tasks.shadowJar)
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