# Tools

这里放本地开发依赖和辅助工具，不提交到发布包以外的业务逻辑。

当前使用：

- `mnn_3.5.0_android_armv7_armv8_cpu_opencl_vulkan/`：MNN Android 预编译库，当前 LLM 主路线。
- `yolo-export/`：YOLO 导出脚本和 Python 环境。

Android 构建会把需要的 `.so` 同步到 generated jniLibs。大模型权重仍放到 App 运行时 models 目录，不放 tools。
