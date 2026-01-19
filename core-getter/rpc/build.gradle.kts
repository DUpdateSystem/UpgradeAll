plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // JSON RPC
    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.7")
    api(project(":core-websdk:data"))

    // Gson for serialization
    implementation("com.google.code.gson:gson:2.13.2")

    // Jackson annotations (for jsonrpc4j compatibility)
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
