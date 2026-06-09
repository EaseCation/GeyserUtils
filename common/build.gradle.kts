plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    api("com.fasterxml.jackson.core:jackson-core:2.15.2")
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    compileOnly("org.jetbrains:annotations:24.0.1")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.compileJava { options.encoding = "UTF-8" }
