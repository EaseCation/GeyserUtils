plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.compileJava { options.encoding = "UTF-8" }

tasks.shadowJar {
    archiveFileName.set("geyserutils-velocity.jar")
}

tasks.build { dependsOn(tasks.shadowJar) }
