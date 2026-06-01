plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mnnode.localruntime"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 26

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

dependencies {
    api(project(":core"))
    api("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    api("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.core:core-ktx:1.16.0")
}
