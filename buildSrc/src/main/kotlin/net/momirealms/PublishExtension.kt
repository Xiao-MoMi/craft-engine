package net.momirealms

import org.gradle.api.publish.maven.MavenPublication

open class PublishExtension {
    open fun applyCommonPom(pub: MavenPublication, customName: String) {
        pub.pom {
            name.set(customName)
            url.set("https://github.com/Xiao-MoMi/craft-engine")
            licenses {
                license {
                    name.set("GNU General Public License v3.0")
                    url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    distribution.set("repo")
                }
            }
        }
    }
}