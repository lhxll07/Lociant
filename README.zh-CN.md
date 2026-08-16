[English](README.md) | **简体中文**

<div align="center">

# Lociant

### 把旧 Android 手机变成本地边缘设备

*模型和设备能力，运行在拥有硬件的地方。*

<img src="docs/media/social-preview.png" alt="Lociant" width="720">

</div>

Lociant 是一个以旧 Android 手机为主要入口的本地边缘运行时。它可以在手机上
运行 GGUF 本地模型，暴露设备能力，报告运行状态，并通过认证的局域网连接其他
Lociant 节点。Linux 电脑和 Rockchip 开发板可以作为控制台、计算节点或无头边缘
节点加入同一个运行时网络。

Lociant 不做聊天客户端，也不运行通用 Agent loop。外部客户端可以通过 MCP
或控制 API 编排单次能力调用，但真正的执行和权限策略始终留在拥有硬件的
设备侧。

## 核心理念

- **让旧硬件获得长期用途**：手机可以成为本地模型节点、屏幕和无障碍节点、
  传感器节点或相机节点，也可以同时承担这些角色。
- **执行留在本地**：GGUF 推理和设备操作都在本机完成，不依赖在线聊天服务。
- **设备按需组网**：手机、电脑和开发板可以在可信局域网中共享模型清单和选定
  的设备工具。
- **权限边界清晰**：工具暴露级别和远程调用策略会在能力执行前由运行时校验。

## 能做什么

| 能力 | 例子 |
|---|---|
| 本地模型 | 导入 GGUF，并通过 llama.cpp 在设备侧运行 |
| 手机工具 | 读取屏幕，通过无障碍点击、输入、打开应用 |
| 设备感知 | 在获得权限后读取传感器、采集相机画面 |
| 局域网互联 | 发现节点，按需共享选定的模型和工具 |
| 外部接入 | 通过 MCP Streamable HTTP 或控制 API 接入 |

## 支持的设备角色

| 设备 | 角色 | 运行方式 |
|---|---|---|
| Android 手机 | 主设备节点和本地模型主机 | Flutter 控制台 + Rust 后端 + Kotlin 设备层 |
| Linux 电脑 | 控制台和文件/进程节点 | Flutter 控制台 + 内置 Rust 后端 |
| Rockchip 开发板 | 低功耗无头边缘节点 | Rust 服务 + RKLLM NPU + 终端 TUI |

所有角色使用同一套控制 API 和 MCP 接口。开发板可以不运行图形界面，手机或
电脑也可以通过局域网查看和控制其他节点。

## 快速开始

### Android 手机（推荐）

1. 下载并安装 [Android APK（arm64-v8a）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk)。
2. 打开 Lociant 并完成引导。Android 13 及以上版本需要允许通知，前台服务
   才能正常启动。
3. 在 **设置 > 安全** 中只开启需要的能力：无障碍、相机、传感器、悬浮窗和
   后台不受限制都按功能选择，并非全部必需。
4. 在不完全可信的网络中使用前设置 API 令牌。应用打开时会启动运行时，主页
   会显示局域网地址以及可复制的 MCP/API 地址。
5. 需要本地推理时，在 **模型** 页面导入 `.gguf` 文件。

Linux、无头开发板、模型格式、节点互联和完整权限说明见
**[配置指南（从零开始）](docs/setup-guide.md)**。

当前 v2.0.1 发布包：

- [Android APK（arm64-v8a）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk)
- [Linux 桌面版（x86_64 tar.gz）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-x86_64.tar.gz)
- [Linux 开发板版（aarch64 tar.gz）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-aarch64.tar.gz)
- [Debian 桌面包（amd64）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant_2.0.1_amd64.deb)
- [Debian 无头节点包（arm64）](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-node_2.0.1_arm64.deb)

## 连接节点

运行时启动后，Rust 后端在 `11434` 端口提供统一的本地 HTTP 接口：

- **MCP Streamable HTTP**：`http://设备IP:11434/mcp`
- **控制 API**：`http://设备IP:11434/api/v1`
- **健康检查**：`http://设备IP:11434/health`

MCP 是外部客户端的接入方式；控制 API 用于查看运行时、设置、模型、节点、
健康状态和直接调用工具。绑定到共享或不完全可信的网络前，请设置
`authToken`，不要把 `11434` 直接暴露到公网。

工具暴露级别是逐级累加的：

- `read`：状态和只读工具；
- `sensor`：只读能力加传感器和环境感知能力；
- `action`：包含设备操作，例如屏幕输入。

运行时会在 HTTP 或 MCP 请求触达设备适配器前，同时检查暴露级别和工具的
`remoteAllowed` 策略。MCP 示例、完整 API 路由和客户端配置见
**[边缘运行时接入指南](docs/edge-runtime-integration.md)**。

## 架构

Rust 后端统一负责控制平面、模型清单、节点网络和工具策略。Android 提供需要
Android 框架的设备能力，Linux 提供桌面能力，无头开发板直接运行后端。

```text
Flutter 控制台（Android / Linux 桌面）
        │ HTTP：/api/v1、/mcp
        ▼
Rust 后端（共享控制平面）
  ├─ 模型清单和本地运行时
  ├─ ToolRegistry 与权限策略
  ├─ 控制 API、MCP 传输和节点网络
  └─ 支持开发板的 RKLLM NPU 运行时
        │ Android 上使用令牌保护的本地 IPC
        ▼
Android 设备层（Kotlin）
  ├─ 前台服务和生命周期
  ├─ 无障碍、传感器、悬浮窗和相机工具
  └─ NCNN 视觉运行时
```

GGUF 推理由本地 llama.cpp 进程完成。NCNN 只负责 Android 相机和视觉能力；
支持的 Rockchip 无头节点使用 RKLLM。

## 文档

- [配置指南](docs/setup-guide.md)：安装、配置和多平台连接
- [边缘运行时接入](docs/edge-runtime-integration.md)：MCP、控制 API 和工具策略
- [架构说明](docs/architecture.md)：数据所有权、边界和部署方式
- [Android 开发说明](apps/android/README.md)：Android 宿主、原生运行时和构建

## 开发

- Flutter UI：`apps/flutter`
- Rust 后端：`apps/rust-backend`
- Android 设备层：`apps/android`

Linux 桌面端需要两个终端：

```bash
cd apps/rust-backend && cargo run
cd apps/flutter && flutter run -d linux
```

构建、安装并启动 Android 应用：

```bash
bash scripts/dev-install.sh
```

Android 构建会把 Rust 后端一起打包，需要 Rust Android target 和
`cargo-ndk`。完整工具链与测试命令见 [Android 开发说明](apps/android/README.md)。

## 范围

Lociant 专注于本地模型、设备能力、受控连接和边缘节点网络。聊天主页、通用
Agent loop、NAS/文件服务器和专用监控应用不属于核心运行时。

## 许可证

[MIT License](LICENSE)
