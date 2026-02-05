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

sourceSets {
    create("integrationTest") {
        kotlin {
            compileClasspath += sourceSets.main.get().output
            runtimeClasspath += sourceSets.main.get().output
        }
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
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

    // Integration test dependencies
    integrationTestImplementation(kotlin("test"))
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    integrationTestImplementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    integrationTestImplementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    integrationTestImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    integrationTestImplementation("io.ktor:ktor-client-mock:3.0.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()
}
