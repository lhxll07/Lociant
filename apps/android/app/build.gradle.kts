plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
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
    namespace = "com.mnnode.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.mnnode.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir("../../../tools/mnn_3.5.0_android_armv7_armv8_cpu_opencl_vulkan")
            assets.srcDir("../../../scenes")
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
    val cameraxVersion = "1.4.2"
    val ktorVersion = "3.1.3"
    val roomVersion = "2.7.2"

    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
