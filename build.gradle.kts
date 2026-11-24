plugins {
    base
}

// EaseCation fork of GeyserExtensionists/GeyserUtils (upstream me.zimzaza4 1.0-SNAPSHOT @ d045474),
// coordinates/versioning per GeyserWorkspace docs/maven-coordinates.md.
subprojects {
    group = "net.easecation.fork.zimzaza4"
    version = "1.0-ec.1"

    apply(plugin = "maven-publish")
    plugins.withId("java") {
        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    description.set("EaseCation fork of GeyserExtensionists GeyserUtils")
                    properties.set(mapOf("upstream.groupId" to "me.zimzaza4", "upstream.version" to "1.0-SNAPSHOT",
                        "upstream.commit" to "d04547488ddb0fb55687142f38fc1bdb83c4c8f0"))
                }
            }
            repositories {
                // EaseCation private repository (Aliyun Yunxiao Packages).
                // Credentials: yunxiaoMavenUsername / yunxiaoMavenPassword in ~/.gradle/gradle.properties.
                maven {
                    name = "yunxiaoMaven"
                    url = uri(
                        if (version.toString().endsWith("-SNAPSHOT"))
                            "https://easecation-cn-hangzhou.devops.aliyuncs.com/packages/api/protocol/maven/14075-snapshot-bjmmxv"
                        else
                            "https://easecation-cn-hangzhou.devops.aliyuncs.com/packages/api/protocol/maven/14075-release-h64s7e"
                    )
                    credentials(PasswordCredentials::class)
                }
            }
        }
    }
}
