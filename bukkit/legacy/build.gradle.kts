repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
    mavenCentral()
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.mojang:datafixerupper:6.0.8")
    compileOnly("com.mojang:authlib:6.0.58")
}

artifacts {
    implementation(tasks.shadowJar)
}