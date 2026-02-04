plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "1.8.22"
    id("io.ktor.plugin") version "3.0.1"
}

group = "com.css"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("com.css.challenge.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.github.ajalt.clikt:clikt:5.0.1")
    implementation("org.slf4j:slf4j-jdk14:2.0.3")

    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.ktor:ktor-client-mock:3.0.1")
    testImplementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    testImplementation("com.github.ajalt.clikt:clikt:5.0.1")
}

tasks.test {
    useJUnitPlatform()
}
