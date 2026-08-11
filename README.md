# Lociant

让一台设备（安卓手机，或 Linux 桌面 / RK3576 开发板）成为一个真正能干活的本地 Agent：本地运行模型、读取屏幕、操作界面、感知环境，也可以通过 MCP 或 OpenAI 兼容接口被电脑上的 Agent 调用。

它适合把旧手机重新利用起来：手机负责模型和实际操作，电脑只负责对话、规划和编排。数据默认留在本地，能力由你在手机里逐项授权。

## 能做什么

- **本地模型**：在手机上运行 LLM / VLM 模型，直接对话和图片理解，断网也能用；开发板支持通过 RKLLM 走 NPU 推理。
- **看屏幕、动手操作**：读取界面、点击、滑动、返回、打开 App，把内容写进任意输入框或粘贴到剪贴板。
- **感知环境**：读取光线、距离、加速度、陀螺仪等传感器，让 Agent 知道手机是在口袋、桌面上还是被拿起，环境是亮是暗、手机在不在动。
- **摄像头与视觉**：拍照、连续画面分析，识别视野里的物体。
- **接入任何 Agent**：通过 MCP 给 Claude、Codex、OpenCode 等使用，也支持 OpenAI 兼容 API；可以在 CPU 和 GPU（OpenCL / Vulkan）推理后端之间切换。
- **云端大脑（可选）**：配置一个 OpenAI 兼容的云端模型后，手机可以直接用它思考和规划，再调用自己的工具——本地优先，云端按需开启。

例如：让 Agent 打开 QQ 看未读消息、总结 B 站动态、查一个 App 里的信息、在输入框里帮你填好一段文字，或者先"感觉"一下手机现在是在口袋里还是桌上，再决定要不要亮屏操作。

## 快速开始

支持三种玩法，按你的设备选对应章节：

- **安卓手机**：完整 Agent（云端/本地模型 + 手机工具）
- **Linux 桌面**：Flutter UI + 内置 Rust 后端，一个压缩包开箱即用
- **RK 开发板（无头）**：systemd 常驻 + RKLLM NPU 推理 + 终端 TUI

所有平台的下载、安装、配置、RKLLM 和多节点互联，见
**[配置指南（从零开始）](docs/setup-guide.md)**。

当前发布包：

[下载 Lociant v2.0.0 APK（Android arm64-v8a）](https://github.com/lhxll07/Lociant/releases/download/v2.0.0/lociant-2.0.0-arm64-v8a-release.apk) ·
[下载 Lociant v2.0.0 Linux（x86_64）](https://github.com/lhxll07/Lociant/releases/download/v2.0.0/lociant-2.0.0-linux-x86_64.tar.gz)

## 架构

一套 Flutter UI + Rust 后端跑在全部平台：Android 只保留设备层（无障碍、
传感器、悬浮窗、本地 MNN 推理），通过本地 IPC 给 Rust 提供工具；桌面端
UI 连接本机 Rust 服务；无头板子直接跑 systemd 服务并通过 RKLLM 走 NPU。
详见 [架构](docs/architecture.md)。

## 开发者

UI 是 Flutter，服务端是 Rust（`apps/rust-backend`），Android 是设备层。
开发、构建和完整接口说明见：

- [架构](docs/architecture.md)
- [配置指南（从零开始）](docs/setup-guide.md)
- [Android 开发说明](apps/android/README.md)
- [Agent 与 MCP 接入（含 OpenAI 兼容 API）](docs/agent-integration.md)
- [控制 API](docs/control-api.md)

## 许可证

[MIT License](LICENSE)
