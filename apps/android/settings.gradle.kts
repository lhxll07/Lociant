pluginManagement {
    repositories {
        // 国内镜像优先，加快依赖下载
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Flutter add-to-app：把 Flutter Gradle 插件作为 included build 提供，
    // 必须在 plugins 块解析之前注册（include_flutter.groovy 里的注册时机太晚）。
    val flutterSdk = runCatching {
        val props = java.util.Properties()
        File(rootDir.parentFile, "flutter/.android/local.properties")
            .inputStream().use { props.load(it) }
        props.getProperty("flutter.sdk")
    }.getOrNull()
    if (flutterSdk != null) {
        includeBuild("$flutterSdk/packages/flutter_tools/gradle")
    }
}

plugins {
    // Flutter add-to-app：让插件子项目能解析 flutter.compileSdkVersion 等扩展
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    // 按 Flutter 模板惯例，AGP/Kotlin 版本在 settings 声明，所有子项目共享
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}

dependencyResolutionManagement {
    // Flutter 插件会添加自己的 maven 仓库；保持 settings 仓库优先即可
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 国内镜像优先，加快依赖下载
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // Flutter 引擎 AAR 发布在 Google 官方存储
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }

        google()
        mavenCentral()
    }
}

rootProject.name = "LociantAndroid"
include(":app")
include(":core")
include(":data")
include(":local-runtime")
include(":phone-tools")

// Flutter UI 模块（apps/flutter），通过官方 add-to-app 方式接入。
apply(from = File(rootDir.parentFile, "flutter/.android/include_flutter.groovy"))
