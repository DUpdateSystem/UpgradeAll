plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    // JSON RPC
    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.7")
    api(project(":core-websdk:data"))
}