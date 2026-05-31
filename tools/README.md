# Tools

这里放本地开发依赖和辅助工具，不提交到发布包以外的业务逻辑。

当前使用：

- `mnn_3.5.0_android_armv7_armv8_cpu_opencl_vulkan/`：MNN Android 预编译库，当前 LLM 主路线。
- `mnn_3.5.0_headers/`：MNN 与 LLM JNI 编译所需的最小头文件集合。
- `ncnn-20260113-android-vulkan/`：NCNN Android Vulkan 预编译包，当前视觉检测路线。

Android 构建会从这里链接 native 依赖。大模型权重仍放到 App 运行时 models 目录，不放 tools。
