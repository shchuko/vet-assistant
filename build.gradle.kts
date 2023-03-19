@file:Suppress("PropertyName")

val kotlin_version: String by project
val kotlinx_serialization: String by project
val ktor_version: String by project
val koin_version: String by project
val koin_logger_version: String by project

plugins {
    kotlin("jvm") version "1.8.10"
    application
    kotlin("plugin.serialization") version "1.8.10"
}

group = "dev.shchuko"
version = "0.0.1"

application {
    mainClass.set("dev.shchuko.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dvet.config.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("io.insert-koin:koin-core:$koin_version")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.0")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    implementation("io.insert-koin:koin-logger-slf4j:$koin_logger_version")

    implementation("com.github.omarmiatello.telegram:client:6.1")

    implementation("org.apache.commons:commons-text:1.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}