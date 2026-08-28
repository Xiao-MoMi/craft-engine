repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
    mavenCentral()
}

dependencies {
    // Platform
    compileOnly(libs.legacy.paper)
    compileOnly(libs.legacy.datafixerupper)
    compileOnly(libs.legacy.authlib)
}

artifacts {
    implementation(tasks.shadowJar)
}
