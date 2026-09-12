plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    // Substituted to the local netease/ec Geyser fork via the workspace composite build.
    compileOnly("org.geysermc.geyser:core:2.9.2-SNAPSHOT")
    compileOnly("org.geysermc.geyser:api:2.9.2-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation(project(":common"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.geysermc.geyser:core:2.9.2-SNAPSHOT")
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
