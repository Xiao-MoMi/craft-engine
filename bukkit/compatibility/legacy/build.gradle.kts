import net.momirealms.nbt
import net.momirealms.paperServer
import net.momirealms.versionOf

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    nbt(project)
    paperServer(project)
    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
    compileOnly(files("libs/flow-nbt-${versionOf("flow-nbt")}.jar"))
    compileOnly(files("libs/awsm-api.jar"))
}
