plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
}