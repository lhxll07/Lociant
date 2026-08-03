plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.lociant.mcp"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    api(project(":core"))
    api("io.ktor:ktor-server-core:3.4.3")
    api("io.ktor:ktor-server-sse:3.4.3")
    api("io.ktor:ktor-server-content-negotiation:3.4.3")
    api("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
    api("io.modelcontextprotocol:kotlin-sdk:0.14.0")
}
