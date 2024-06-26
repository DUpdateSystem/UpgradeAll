plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
}