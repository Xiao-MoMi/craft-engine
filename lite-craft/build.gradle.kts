plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "net.litecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("net.bytebuddy:byte-buddy:1.14.12")
    compileOnly("io.netty:netty-all:4.1.101.Final")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    targetCompatibility = "21"
    sourceCompatibility = "21"
}
