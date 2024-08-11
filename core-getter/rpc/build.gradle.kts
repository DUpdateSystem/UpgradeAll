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
    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.6")
    api(project(":core-websdk:data"))
}
