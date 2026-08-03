plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val buildWebUi by tasks.registering(Exec::class) {
    workingDir = file("src/main/web-src")
    commandLine("python", "build.py")
    inputs.dir(file("src/main/web-src"))
    outputs.files(
        file("src/main/assets/web/app.js"),
        file("src/main/assets/web/index.html"),
        file("src/main/assets/web/styles.css"),
    )
}

android {
    namespace = "io.lociant.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.lociant.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 10100
        versionName = "1.1.0"
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

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
            )
        }
        jniLibs {
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libMNN.so",
                "**/libMNN_Express.so",
                "**/libMNN_Vulkan.so",
                "**/libMNN_CL.so",
                "**/libMNNOpenCV.so",
                "**/libMNNAudio.so",
                "**/libmnncore.so",
                "**/libllm.so",
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn(buildWebUi)
}

dependencies {
    val ktorVersion = "3.4.3"

    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":local-runtime"))
    implementation(project(":phone-tools"))
    implementation(project(":mcp"))
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
}
