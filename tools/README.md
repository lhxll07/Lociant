# Tools

这里放本地开发依赖和辅助工具，不提交到发布包以外的业务逻辑。

当前使用：

- `mnn_3.6.1_android_armv7_armv8_cpu_opencl_vulkan/`：MNN Android 预编译库，arm64 支持 16 KB 内存页。
- `mnn_3.6.1_headers/`：与预编译库匹配的 MNN 与 LLM JNI 头文件集合。
- `ncnn-20260113-android-vulkan/`：NCNN Android Vulkan 预编译包，当前视觉检测路线。

Android 构建会从这里链接 native 依赖。大模型权重仍放到 App 运行时 models 目录，不放 tools。
