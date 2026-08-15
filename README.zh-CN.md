[English](README.md) | **简体中文**

# Lociant —— 把身边每一台设备，都变成边缘运行时

> 你的下一台 AI 设备，可能不是新电脑，而是抽屉里那台旧手机。

<img src="docs/media/social-preview.png" alt="Lociant" width="720">

Lociant 让一台普通设备（旧手机、Linux 电脑、RK 开发板）成为可控制的边缘
运行时：本地运行模型、暴露设备能力、报告运行状态，并通过局域网连接其他
节点。Claude、Codex 等外部客户端可以通过 MCP 编排这些能力，但执行与策略
始终留在设备侧。

## 它能帮你做什么

- **让旧手机常驻工作**：向可信客户端提供屏幕、无障碍、传感器、相机和应用能力。
- **真正运行在边缘**：手机使用 llama.cpp 运行 GGUF，Rockchip 开发板使用 RKLLM NPU，不依赖在线聊天服务。
- **设备可以组网**：手机、电脑、开发板在认证的局域网中共享模型清单和设备工具。
- **接入任意编排客户端**：通过 MCP 和控制 API 连接 Claude、Codex、OpenCode 或自建客户端。
- **策略靠近硬件**：远程工具的暴露级别和调用权限在工具真正执行前由设备侧校验。

最终得到的是一个小而清晰的运行时：客户端可以让它打开应用、读取屏幕、检查
传感器、抓取相机画面，或调用一次设备侧本地模型。

## 三种玩法

| 设备 | 玩法 |
|---|---|
| 安卓手机 | 设备运行时：本地 GGUF/llama.cpp + 手机工具（看屏、点击、传感器、相机） |
| Linux 桌面（x86_64） | Flutter 控制台 + 内置 Rust 边缘后端 |
| RK 开发板（Armbian） | systemd 边缘节点 + RKLLM NPU + 终端 TUI |

## 快速开始

支持三种玩法，按你的设备选对应章节：

- **安卓手机**：设备运行时（本地模型 + 手机工具）
- **Linux 桌面**：Flutter UI + 内置 Rust 后端，一个压缩包开箱即用
- **RK 开发板（无头）**：systemd 常驻 + RKLLM NPU 推理 + 终端 TUI

所有平台的下载、安装、配置、RKLLM 和多节点互联，见
**[配置指南（从零开始）](docs/setup-guide.md)**。

当前发布包：

[下载 Lociant v2.0.1 APK（Android arm64-v8a）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk) ·
[下载 Linux x86_64 桌面版](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-x86_64.tar.gz) ·
[下载 Linux aarch64 开发板版](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-aarch64.tar.gz)

Debian / Ubuntu 也可直接安装
[x86_64 桌面 DEB](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant_2.0.1_amd64.deb) 或
[arm64 无头节点 DEB](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-node_2.0.1_arm64.deb)。

## 架构

一套 Flutter UI + Rust 后端跑在全部平台：Android 只保留设备层（无障碍、
传感器、悬浮窗、NCNN 视觉），通过本地 IPC 给 Rust 提供工具；GGUF 模型由
llama.cpp 进程运行；桌面端
UI 连接本机 Rust 服务；无头板子直接跑 systemd 服务并通过 RKLLM 走 NPU。
详见 [架构](docs/architecture.md)。

## 开发者

UI 是 Flutter，服务端是 Rust（`apps/rust-backend`），Android 是设备层。
开发、构建和完整接口说明见：

- [架构](docs/architecture.md)
- [配置指南（从零开始）](docs/setup-guide.md)
- [Android 开发说明](apps/android/README.md)
- [边缘运行时接入（MCP、控制接口、工具策略）](docs/agent-integration.md)

## 许可证

[MIT License](LICENSE)
