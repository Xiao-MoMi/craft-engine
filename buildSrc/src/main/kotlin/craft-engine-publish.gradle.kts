import java.net.URI

plugins {
    `maven-publish`
}

val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

publishing {
    repositories {
        maven {
            val repoName = if (isSnapshot) "snapshots" else "releases"
            name = "XiaoMoMi"
            url = URI("https://repo.momirealms.net/$repoName")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
    }
}
