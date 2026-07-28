# Native Development Tools

该目录保存 Android Native 构建依赖。这里只放与编译、链接直接相关的预编译库和头文件，不放运行时下载的大模型权重，也不在这里实现业务逻辑。

## 当前依赖

| 目录 | 用途 |
|---|---|
| `mnn_3.6.1_android_armv7_armv8_cpu_opencl_vulkan/` | MNN 3.6.1 Android 预编译库，包含 `arm64-v8a` 与 `armeabi-v7a`；arm64 支持 16 KB 内存页 |
| `mnn_3.6.1_headers/` | 与上述二进制严格匹配的 MNN、Express、Plugin 和 LLM JNI 头文件 |
| `ncnn-20260113-android-vulkan/` | NCNN Android Vulkan 预编译包，用于当前视觉检测链路 |

Android 构建通过 `apps/android/local-runtime/build.gradle.kts` 和 `local-runtime/src/main/cpp/CMakeLists.txt` 引用这些目录。升级 Native 依赖时，必须在同一提交中同步修改 Gradle、CMake、目录说明和对应头文件，不能混用不同 MNN 版本的库与头文件。

## 模型文件边界

LLM/VLM 权重不属于构建依赖。它们应由 App 的模型导入或模型市场流程写入：

```text
Android/data/io.lociant.android/files/models/<model-id>/
```

不要把用户模型、下载缓存、安装临时目录或设备导出的运行数据提交到 `tools/`。

## 验证

升级 Native 库后至少执行：

```bash
cd apps/android
bash gradlew :app:assembleDebug :app:lintDebug
```

检查生成 APK 中的 arm64 Native 文件是否满足 16 KB 对齐：

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/zipalign -c -P 16 4 \
  apps/android/app/build/outputs/apk/debug/app-debug.apk
```

同时应在 `arm64-v8a` 和 `armeabi-v7a` 设备或模拟器上完成加载测试；只通过编译不能证明 JNI 符号、ABI 或运行时依赖完全匹配。
