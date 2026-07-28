<div align="center">

# Lociant

### Android-native local AI runtime and phone capability layer for agents.

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android-3DDC84.svg)](apps/android)
[![OpenAI Compatible](https://img.shields.io/badge/OpenAI-compatible-111827.svg)](docs/openai-compatible.md)
[![MCP](https://img.shields.io/badge/MCP-Streamable%20HTTP-0F766E.svg)](docs/agent-integration.md)

[English](#english) | [中文](#中文)

</div>

---

## English

Lociant turns an Android phone into a local AI and device-capability node that agents can call over a trusted LAN. It runs local LLM/VLM models, exposes Android tools, and provides OpenAI-compatible, Ollama-style, and MCP interfaces from one foreground runtime.

Lociant is not a complete agent framework and does not replace Codex, OpenCode, RikkaHub, Pi, or your own automation system. Planning, workspace files, shell commands, code edits, and long-running task state remain the responsibility of the client. Lociant owns the phone-local model and Android capability boundary.

### Current Scope

- Local LLM/VLM inference through OpenAI-compatible chat and Ollama-style chat endpoints.
- MCP Streamable HTTP and a shared tool registry for direct HTTP or model tool calls.
- Android tools for device status, clipboard, app launch, accessibility hierarchy, optional screenshots, node click, tap, swipe, back, home, recent apps, notifications, quick settings, and UI idle wait.
- Camera vision through CameraX, including frame capture and local VLM input.
- Model discovery, ZIP import, remote model market, installation progress, and model deletion.
- Persistent sessions, messages, events, and namespaced JSON key-value storage.
- A visible foreground runtime with a notification, optional Runtime Window, permission controls, and an in-app WebView UI.

The application has deliberately removed the old Scene Pack/scene iframe runtime, embedded desktop ACP client and node switching, and Gadgetbridge Web/Bridge UI. The `/v1/scenes` endpoint is no longer available. Desktop agents should connect through the public HTTP or MCP boundary instead of being embedded in the Android app.

### Quick Start

1. Install the Android app and open it.
2. Grant only the Android permissions needed by the tools you plan to use.
3. Start Runtime and note the LAN address shown by the app, for example `http://10.238.125.4:11434`.
4. Keep the service on a trusted LAN. If API Token is enabled, send `Authorization: Bearer <token>` from LAN clients.
5. Configure an OpenAI-compatible or MCP-capable client using the examples below.

OpenAI-compatible provider:

```text
Base URL: http://<phone-ip>:11434/v1
Model: one id returned by GET /v1/models
API Key: blank, or any non-empty value if the client requires one
```

MCP client:

```text
Name: Lociant
Transport: Streamable HTTP
URL: http://<phone-ip>:11434/mcp
Headers: empty, or Authorization: Bearer <token>
```

Useful first tool calls are `runtime_status`, `device_status`, `ui_screen_state`, and `camera_capture`. Start the vision runtime before using the camera path. For a VLM request, call `llm_chat` with `useScreenFrame`, `useCameraFrame`, or an explicit image data URL.

### HTTP API

All protected endpoints accept the configured bearer token. `/health` remains public for discovery and diagnostics.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/health` | Runtime health, address, and capability discovery |
| `GET` | `/v1/models` | OpenAI-style installed and built-in model list |
| `GET` | `/v1/models/full?refresh=true` | Detailed model status; `refresh=true` invalidates the directory snapshot |
| `GET` | `/v1/models/market?q=<query>&refresh=true` | Search or refresh the remote model catalog |
| `POST` | `/v1/models/market/{modelId}/install` | Start an asynchronous market installation |
| `GET` | `/v1/models/market/{modelId}/progress` | Read installation/download progress |
| `POST` | `/v1/models/{modelId}/delete` | Delete an externally installed model |
| `GET` | `/v1/sessions` | Read persisted session summaries |
| `GET` | `/v1/store/{namespace}/{key}` | Read a JSON key-value entry |
| `POST` | `/v1/store/{namespace}/{key}` | Store `{ "value": ... }` atomically |
| `POST` | `/v1/runtime/{command}` | Start, stop, configure, or inspect the Android runtime |
| `GET` | `/v1/tools` | List the shared local tool manifest |
| `POST` | `/v1/tools/{name}/call` | Invoke a local tool with `{ "arguments": { ... } }` |
| `POST` | `/v1/chat/completions` | OpenAI-style chat, streaming, images, and tool calls |
| `POST` | `/api/chat` | Ollama-style chat |
| `GET` | `/v1/chat/status/{requestId}` | Inspect an asynchronous chat request |
| `GET` | `/v1/chat/queue` | Inspect the inference queue |
| `GET`, `POST` | `/mcp` | MCP Streamable HTTP transport |

The public response diagnostics field `mnnode` and request header `X-MNNode-Session-Id` are intentionally retained for compatibility with existing clients and scripts.

### Model Management

The in-app Models page supports local packages and the model market:

- To import a model, select a ZIP containing exactly one MNN model root with `config.json` and its referenced model, weight, and tokenizer files. Import is staged, path-validated, and replaces an existing model only after the new package is ready.
- Market installation runs asynchronously. Use the progress endpoint or the in-app progress indicator until the state becomes `done` or `error`.
- `GET /v1/models/full` uses a thread-safe model-directory snapshot. Import, market installation, deletion, and an explicit `refresh=true` invalidate that snapshot.
- The bundled native runtime is MNN 3.6.1 for `arm64-v8a` and `armeabi-v7a`. The arm64 package supports Android devices using 16 KB memory pages.

### Runtime Architecture

```text
Web UI / LAN client / agent client
  -> Ktor HTTP API or MCP
    -> ApiServerController
      -> ChatController / ToolRegistry / LocalStore
        -> MNN / NCNN / CameraX / Room / Android services
```

The Android modules have narrow responsibilities:

| Module | Responsibility |
|---|---|
| `:app` | WebView shell, Android entry points, foreground service, Runtime Window, and HTTP composition |
| `:core` | Shared constants, chat/tool data types, and protocol-neutral contracts |
| `:data` | Room sessions/messages/events and the local JSON store |
| `:local-runtime` | MNN/NCNN inference, model management, CameraX vision, and native CMake code |
| `:phone-tools` | Android device, accessibility, local-model, and vision tools |
| `:mcp` | MCP Streamable HTTP protocol adapter |

Performance-sensitive state is kept close to its owner. `ModelManager` caches an immutable model-directory snapshot, `LocalStore` loads JSON once and serves synchronized in-memory reads with `AtomicFile` writes, and the WebView polls a lightweight state signature instead of serializing the full runtime state on every tick.

### Upgrade Notes

The Room database is version 2. The v1 to v2 migration preserves sessions, messages, and events, but drops the unused legacy `assets` table and its data. Back up that table before upgrading only if a private fork still depends on it.

### Build

Requirements: JDK 17, Android SDK 36, Android Build Tools 36.0.0, NDK 28.2.13676358, and CMake 3.22.1. The project currently uses Android Gradle Plugin 8.13.0 and Gradle 8.13.

Linux/macOS:

```bash
cd apps/android
bash gradlew :app:assembleDebug
```

Windows:

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

Debug APK:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

Full local verification:

```bash
cd apps/android
bash gradlew :app:assembleDebug :app:lintDebug
cd ../..
python3 -m py_compile scripts/*.py
git diff --check
```

Check 16 KB APK alignment with Android Build Tools:

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/zipalign -c -P 16 4 \
  apps/android/app/build/outputs/apk/debug/app-debug.apk
```

Probe a running phone:

```bash
python3 scripts/lociant_test.py quick \
  --base-url http://<phone-ip>:11434 \
  --chat
```

### Documentation

- [Unified Architecture](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)
- [Agent Integration](docs/agent-integration.md)
- [Android Project](apps/android/README.md)

---

## 中文

Lociant 把 Android 手机变成一个可在可信局域网内调用的本地 AI 与设备能力节点。它在同一个前台运行时中执行本地 LLM/VLM、暴露 Android 工具，并提供 OpenAI-compatible、Ollama 风格和 MCP 接口。

Lociant 不是完整的 Agent 框架，也不替代 Codex、OpenCode、RikkaHub、Pi 或你自己的自动化系统。规划、工作区文件、Shell 命令、代码编辑和长期任务状态仍由客户端负责；Lociant 只负责手机本地模型和 Android 能力边界。

### 当前能力

- 通过 OpenAI-compatible chat 和 Ollama 风格接口运行本地 LLM/VLM。
- 提供 MCP Streamable HTTP 和统一工具注册表，支持直接 HTTP 调用或模型工具调用。
- 提供设备状态、剪贴板、打开 App、无障碍层级、可选截图、节点点击、点击、滑动、返回、主页、最近任务、通知栏、快捷设置和 UI 空闲等待等 Android 工具。
- 通过 CameraX 提供摄像头视觉、画面捕获和本地 VLM 图像输入。
- 提供模型发现、ZIP 导入、远程模型市场、安装进度和模型删除。
- 持久化会话、消息、事件和带命名空间的 JSON 键值数据。
- 提供可见的前台运行时，包括通知、可选悬浮运行窗口、权限控制和 App 内 WebView UI。

项目已明确移除旧 Scene Pack/Scene iframe 运行时、内嵌桌面 ACP 客户端与节点切换，以及 Gadgetbridge Web/Bridge UI。`/v1/scenes` 端点不再存在。桌面 Agent 应通过公开 HTTP 或 MCP 边界接入，不再嵌入 Android App。

### 快速开始

1. 安装并打开 Android App。
2. 仅授予计划使用的工具所需的 Android 权限。
3. 启动 Runtime，记录 App 显示的局域网地址，例如 `http://10.238.125.4:11434`。
4. 只在可信局域网中运行服务。启用 API Token 后，局域网客户端需发送 `Authorization: Bearer <token>`。
5. 按下面示例配置 OpenAI-compatible 或 MCP 客户端。

OpenAI-compatible provider：

```text
Base URL: http://<phone-ip>:11434/v1
Model: 从 GET /v1/models 返回值中选择
API Key: 留空，或按客户端要求填写任意非空值
```

MCP 客户端：

```text
名称：Lociant
传输：Streamable HTTP
URL：http://<phone-ip>:11434/mcp
Headers：留空，或填写 Authorization: Bearer <token>
```

建议先测试 `runtime_status`、`device_status`、`ui_screen_state` 和 `camera_capture`。使用摄像头链路前需要先启动 vision runtime；VLM 请求可以通过 `llm_chat` 传入 `useScreenFrame`、`useCameraFrame` 或显式图片 data URL。

### HTTP API

所有受保护端点均接受已配置的 bearer token。`/health` 保持公开，便于服务发现和诊断。

| 方法 | 端点 | 用途 |
|---|---|---|
| `GET` | `/health` | 获取运行状态、地址和能力发现信息 |
| `GET` | `/v1/models` | 获取 OpenAI 风格的已安装及内置模型列表 |
| `GET` | `/v1/models/full?refresh=true` | 获取完整模型状态；`refresh=true` 会使目录快照失效并重新扫描 |
| `GET` | `/v1/models/market?q=<query>&refresh=true` | 搜索或刷新远程模型目录 |
| `POST` | `/v1/models/market/{modelId}/install` | 启动异步模型市场安装任务 |
| `GET` | `/v1/models/market/{modelId}/progress` | 查询下载和安装进度 |
| `POST` | `/v1/models/{modelId}/delete` | 删除外部安装的模型 |
| `GET` | `/v1/sessions` | 读取持久化会话摘要 |
| `GET` | `/v1/store/{namespace}/{key}` | 读取 JSON 键值数据 |
| `POST` | `/v1/store/{namespace}/{key}` | 原子写入 `{ "value": ... }` |
| `POST` | `/v1/runtime/{command}` | 启动、停止、配置或检查 Android runtime |
| `GET` | `/v1/tools` | 获取统一的本地工具清单 |
| `POST` | `/v1/tools/{name}/call` | 使用 `{ "arguments": { ... } }` 调用本地工具 |
| `POST` | `/v1/chat/completions` | OpenAI 风格聊天、流式响应、图片和工具调用 |
| `POST` | `/api/chat` | Ollama 风格聊天 |
| `GET` | `/v1/chat/status/{requestId}` | 查询异步聊天请求状态 |
| `GET` | `/v1/chat/queue` | 查询推理队列 |
| `GET`, `POST` | `/mcp` | MCP Streamable HTTP 传输端点 |

公开响应诊断字段 `mnnode` 和请求头 `X-MNNode-Session-Id` 会继续保留，以兼容已有客户端和脚本。

### 模型管理

App 内的“模型”页面同时支持本地模型包和模型市场：

- 本地导入请选择 ZIP 文件。ZIP 中必须包含且仅包含一个带 `config.json` 的 MNN 模型根目录，以及配置引用的模型、权重和 tokenizer 文件。导入过程会在临时目录中完成路径校验和解压，新模型就绪后才会替换同名旧模型。
- 模型市场安装为异步任务。请通过进度端点或 App 内进度条等待状态变为 `done` 或 `error`。
- `GET /v1/models/full` 使用线程安全的模型目录快照。模型导入、市场安装、删除以及显式 `refresh=true` 都会让快照失效。
- 内置 Native Runtime 已升级到 MNN 3.6.1，包含 `arm64-v8a` 和 `armeabi-v7a`；其中 arm64 包支持使用 16 KB 内存页的 Android 设备。

### 运行架构

```text
Web UI / 局域网客户端 / Agent 客户端
  -> Ktor HTTP API 或 MCP
    -> ApiServerController
      -> ChatController / ToolRegistry / LocalStore
        -> MNN / NCNN / CameraX / Room / Android services
```

Android 模块职责保持收敛：

| 模块 | 职责 |
|---|---|
| `:app` | WebView 壳层、Android 入口、前台服务、Runtime Window 和 HTTP 组装 |
| `:core` | 公共常量、聊天/工具数据类型和协议无关契约 |
| `:data` | Room 会话/消息/事件与本地 JSON Store |
| `:local-runtime` | MNN/NCNN 推理、模型管理、CameraX 视觉和 Native CMake 代码 |
| `:phone-tools` | Android 设备、无障碍、本地模型和视觉工具 |
| `:mcp` | MCP Streamable HTTP 协议适配器 |

性能敏感状态由各自模块直接管理。`ModelManager` 缓存不可变的模型目录快照；`LocalStore` 启动时只读盘一次，后续使用同步内存读取和 `AtomicFile` 写入；WebView 轮询使用轻量状态签名，不再每次序列化完整运行状态。

### 升级说明

Room 数据库版本已升级到 v2。v1 到 v2 的迁移会保留会话、消息和事件，但会删除未被使用的旧 `assets` 表及其中数据。如果私有分支仍依赖该表，请在升级前自行备份。

### 构建

环境要求：JDK 17、Android SDK 36、Android Build Tools 36.0.0、NDK 28.2.13676358 和 CMake 3.22.1。项目当前使用 Android Gradle Plugin 8.13.0 与 Gradle 8.13。

Linux/macOS：

```bash
cd apps/android
bash gradlew :app:assembleDebug
```

Windows：

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

Debug APK：

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

完整本地验证：

```bash
cd apps/android
bash gradlew :app:assembleDebug :app:lintDebug
cd ../..
python3 -m py_compile scripts/*.py
git diff --check
```

使用 Android Build Tools 检查 APK 的 16 KB 对齐：

```bash
$ANDROID_SDK_ROOT/build-tools/36.0.0/zipalign -c -P 16 4 \
  apps/android/app/build/outputs/apk/debug/app-debug.apk
```

连接真机后执行快速探测：

```bash
python3 scripts/lociant_test.py quick \
  --base-url http://<phone-ip>:11434 \
  --chat
```

### 文档

- [统一架构](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)
- [Agent 集成](docs/agent-integration.md)
- [Android 工程](apps/android/README.md)
