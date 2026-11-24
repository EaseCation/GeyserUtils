plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

repositories {
    // EaseCation private repository (Aliyun Yunxiao Packages), only for net.easecation.* coordinates.
    // Credentials: yunxiaoMavenUsername / yunxiaoMavenPassword in ~/.gradle/gradle.properties.
    exclusiveContent {
        forRepository {
            maven("https://easecation-cn-hangzhou.devops.aliyuncs.com/packages/api/protocol/maven/14075-release-h64s7e") {
                name = "yunxiaoMaven"
                credentials(PasswordCredentials::class)
            }
        }
        filter { includeGroupByRegex("net\\.easecation(\\..*)?") }
    }
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("net.easecation.fork.geysermc.floodgate:api:2.2.5-ec.2") // EaseCation Floodgate fork
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation(project(":geyserutils-common"))
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.compileJava { options.encoding = "UTF-8" }

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("project" to mapOf("version" to project.version.toString()))
    }
}

tasks.shadowJar {
    archiveFileName.set("geyserutils-spigot.jar")
    relocate("com.fasterxml.jackson", "me.zimzaza4.geyserutils.relocations.jackson")
}

tasks.build { dependsOn(tasks.shadowJar) }
