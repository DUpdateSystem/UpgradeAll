plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    api(project(":core-websdk:data"))

    // Gson for serialization
    implementation(libs.gson)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Ktor WebSocket client for JSON-RPC
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
}
