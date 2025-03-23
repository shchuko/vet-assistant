@file:Suppress("PropertyName")

val kotlin_version: String by project
val kotlinx_serialization: String by project
val koin_version: String by project
val koin_logger_version: String by project
val sl4j_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    application
    kotlin("plugin.serialization") version "1.9.10"
}

group = "dev.shchuko"
version = "0.0.1"

application {
    mainClass.set("dev.shchuko.vet_assistant.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dvet.config.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    runtimeOnly("io.insert-koin:koin-core:$koin_version")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("ru.homyakin:iuliia-java:1.8") // TODO get rid of vulnerable dependency
    implementation("io.insert-koin:koin-logger-slf4j:$koin_logger_version")

    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")

    implementation("org.apache.commons:commons-text:1.10.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.17.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")

    implementation("com.h2database:h2:2.2.224")
    implementation("com.vk.api:sdk:1.0.16")

    runtimeOnly("org.postgresql:postgresql:42.7.3")

    implementation("org.flywaydb:flyway-core:9.22.2")

    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}