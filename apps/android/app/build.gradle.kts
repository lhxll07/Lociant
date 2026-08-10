import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.lociant.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.lociant.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 10101
        versionName = "1.1.1"
    }

    buildTypes {
        debug {
            // 本地联调只需要 arm64-v8a；Flutter 调试引擎的 libflutter.so 占了
            // APK 的大头，限制 ABI 后 debug 包从 ~1.4GB 降到 ~0.5GB，安装快很多。
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
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
            // 让 .so 在安装时落地到 nativeLibraryDir（apk_data_file 上下文），
            // 应用域才能 exec Rust 服务二进制；默认的不落地模式只支持 dlopen。
            useLegacyPackaging = true
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
            // Flutter debug 引擎自带的 Vulkan 验证层仅用于 GPU 调试，体积 ~80MB
            excludes += listOf("**/libVkLayer_khronos_validation.so")
        }
    }
}

dependencies {
    val ktorVersion = "3.4.3"

    implementation(project(":flutter"))
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":local-runtime"))
    implementation(project(":phone-tools"))
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
}

// ---- Rust server (unified backend) ----
//
// Cross-compiles apps/rust-backend and packages the binary as
// liblociant_server.so in jniLibs. Requires the Rust Android toolchain:
//   rustup target add aarch64-linux-android
//   cargo install cargo-ndk   (or yay -S cargo-ndk)
val rustBackendDir = rootProject.file("../rust-backend")
val rustNdkRoot = providers.environmentVariable("ANDROID_NDK_HOME").orElse(
    providers.provider {
        val ndkDir = File(android.sdkDirectory, "ndk")
        ndkDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?.absolutePath
            ?: android.sdkDirectory.absolutePath
    }
).get()

tasks.register<Exec>("rustServerBinary") {
    group = "build"
    description = "Cross-compiles the Rust server for arm64-v8a"
    workingDir = rustBackendDir
    environment("ANDROID_NDK_HOME", rustNdkRoot)
    commandLine("cargo", "ndk", "-t", "arm64-v8a", "build", "--release")
}

tasks.register<Copy>("rustServerJniLib") {
    group = "build"
    description = "Copies the Rust server binary into jniLibs"
    dependsOn("rustServerBinary")
    from(rustBackendDir.resolve("target/aarch64-linux-android/release/lociant-server"))
    into(file("src/main/jniLibs/arm64-v8a"))
    rename { "liblociant_server.so" }
}

tasks.named("preBuild") {
    dependsOn("rustServerJniLib")
}
