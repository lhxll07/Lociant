import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releasePassword = System.getenv("LOCIANT_KEYSTORE_PASSWORD")

android {
    namespace = "io.lociant.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.lociant.android" +
            if (System.getenv("LOCIANT_CLONE") == "1") ".clone" else ""
        minSdk = 26
        targetSdk = 36
        versionCode = 20002
        versionName = "2.0.2"
    }

    signingConfigs {
        if (!releasePassword.isNullOrBlank()) create("release") {
            // Keystore lives outside the repo (~/keys/lociant-release.jks).
            // Password comes from LOCIANT_KEYSTORE_PASSWORD so it never
            // lands in git; point LOCIANT_KEYSTORE elsewhere to override.
            storeFile = file(
                System.getenv("LOCIANT_KEYSTORE")
                    ?: "${System.getProperty("user.home")}/keys/lociant-release.jks",
            )
            storePassword = releasePassword
            keyAlias = "lociant"
            keyPassword = releasePassword
        }
    }

    buildTypes {
        debug {
            // 本地联调只需要 arm64-v8a；Flutter 调试引擎的 libflutter.so 占了
            // APK 的大头，限制 ABI 后 debug 包从 ~1.4GB 降到 ~0.5GB，安装快很多。
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
        release {
            // Debug builds and unit tests must not require release secrets.
            // A release build still fails clearly below when signing is not
            // configured, before an unsigned artifact can be published.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            // 与 debug 一致：仅 arm64-v8a（Rust server 只交叉编译该 ABI，
            // 且能显著减小 APK 体积）。
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
            )
            // Flutter debug 引擎自带的 Vulkan 验证层仅用于 GPU 调试，体积 ~80MB
            excludes += listOf("**/libVkLayer_khronos_validation.so")
        }
    }
}

tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }
    .configureEach {
        doFirst {
            check(!releasePassword.isNullOrBlank()) {
                "LOCIANT_KEYSTORE_PASSWORD is required for release builds"
            }
        }
    }

dependencies {
    implementation(project(":flutter"))
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":local-runtime"))
    implementation(project(":phone-tools"))
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.0")
}

// ---- Rust server (unified backend) ----
//
// Cross-compiles apps/rust-backend and packages the binary as
// liblociant_server.so in jniLibs. Requires the Rust Android toolchain:
//   rustup target add aarch64-linux-android
//   cargo install cargo-ndk   (or yay -S cargo-ndk)
val rustBackendDir = rootProject.file("../rust-backend")
val rustServerBinaryOutput = rustBackendDir.resolve("target/aarch64-linux-android/release/lociant-server")
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
    inputs.files(
        rustBackendDir.resolve("Cargo.toml"),
        rustBackendDir.resolve("Cargo.lock"),
    )
    inputs.dir(rustBackendDir.resolve("crates"))
    outputs.file(rustServerBinaryOutput)
    // -P 26: bionic getifaddrs 需要 API 24+（mdns/if-addrs 依赖）
    commandLine("cargo", "ndk", "-t", "arm64-v8a", "-P", "26", "build", "--release")
}

tasks.register<Copy>("rustServerJniLib") {
    group = "build"
    description = "Copies the Rust server binary into jniLibs"
    dependsOn("rustServerBinary")
    from(rustServerBinaryOutput)
    into(file("src/main/jniLibs/arm64-v8a"))
    rename { "liblociant_server.so" }
    inputs.file(rustServerBinaryOutput)
    outputs.file(file("src/main/jniLibs/arm64-v8a/liblociant_server.so"))
}

val llamaAndroidDir = rootProject.file("../../tools/llama-android/arm64-v8a")
val llamaServerJniLibOutput = file("src/main/jniLibs/arm64-v8a/libllama_server.so")

tasks.register<Copy>("llamaServerJniLib") {
    group = "build"
    description = "Copies an optional prebuilt Android llama-server binary and its .so libraries into jniLibs"
    onlyIf { File(llamaAndroidDir, "llama-server").exists() }
    from(llamaAndroidDir)
    into(file("src/main/jniLibs/arm64-v8a"))
    rename { name -> if (name == "llama-server") "libllama_server.so" else name }
    inputs.dir(llamaAndroidDir)
    outputs.file(llamaServerJniLibOutput)
}

tasks.named("preBuild") {
    dependsOn("rustServerJniLib", "llamaServerJniLib")
}
