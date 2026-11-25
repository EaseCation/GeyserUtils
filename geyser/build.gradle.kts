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
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    // EaseCation (NetEase) Geyser fork from Yunxiao; the workspace substitutes it with the local project.
    compileOnly("net.easecation.fork.geysermc.geyser:core:2.9.2-ec.3")
    compileOnly("net.easecation.fork.geysermc.geyser:api:2.9.2-ec.3")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation(project(":geyserutils-common"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("net.easecation.fork.geysermc.geyser:core:2.9.2-ec.3")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

java {
    // Must read the fork's Java 21 :core bytecode.
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.compileJava { options.encoding = "UTF-8" }

tasks.shadowJar {
    archiveFileName.set("geyserutils-geyser.jar")
    relocate("com.fasterxml.jackson", "me.zimzaza4.geyserutils.relocations.jackson")
}

tasks.build { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }
