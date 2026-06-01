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

Lociant turns an Android phone into a local AI node that agents can call over LAN.

It is not a full agent framework and does not try to replace Codex, OpenCode, RikkaHub, Pi, or your own automation system. Lociant sits below them: it provides phone-local models, camera vision, Android state, screen tools, notifications, storage, and a standard API surface.

## What It Provides

- Local LLM/VLM inference through an OpenAI-compatible API and Ollama-style endpoints.
- MCP Streamable HTTP server for direct agent integration.
- Android-side tools: device status, clipboard, app launch, notifications, compact screen state, node click, screenshot-backed screen capture, tap, swipe, back/home/recent apps.
- Camera vision: start vision runtime, capture camera frames, and pass images to local VLM models.
- Persistent sessions and local storage for phone-side context.
- ACP desktop node support, so the phone can act as a control surface for a Codex process running on a computer.
- A visible Android runtime with foreground service, runtime window, permissions, and in-app WebView settings.

## Quick Start

1. Install the Android app.
2. Start Runtime in the app.
3. Confirm the phone shows a LAN address, for example:

```text
http://10.238.125.4:11434
```

4. Add Lociant to an MCP-capable agent client:

```text
Name: Lociant
Transport: Streamable HTTP
URL: http://<phone-ip>:11434/mcp
Headers: empty
```

If API Token is enabled, add:

```text
Authorization: Bearer <token>
```

5. Enable the MCP tools in the current chat and test:

```text
Call runtime_status.
Call device_status.
Read the current phone UI with ui_screen_state.
Ask the local VLM about the current screen with llm_chat and useScreenFrame.
Start vision, then call camera_capture.
```

## OpenAI-Compatible API

Use the phone as a local model provider:

```text
Base URL: http://<phone-ip>:11434/v1
Model: one id from GET /v1/models
API Key: blank, or any non-empty value if your client requires one
```

Useful endpoints:

| Endpoint | Purpose |
|---|---|
| `/v1/chat/completions` | OpenAI-style chat and streaming |
| `/v1/models` | Installed and built-in models |
| `/v1/tools` | Local tool manifest |
| `/v1/tools/{name}/call` | Direct local tool call |
| `/mcp` | MCP Streamable HTTP endpoint |
| `/health` | Runtime health and discovery |

## Build

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

Debug APK:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

## Design Boundary

Lociant owns phone-side capabilities. Desktop agents should still own planning, workspace files, shell commands, code edits, and long-running task state.

Keep the split simple:

```text
Agent client
  -> planning, coding, workspace tools, UI
  -> OpenAI API / MCP
  -> Lociant on Android
      -> local LLM/VLM
      -> camera, screen, Android tools
      -> notifications, sessions, storage
```

Android code is split by capability: `:core`, `:data`, `:local-runtime`, `:phone-tools`, `:mcp`, `:acp`, with `:app` kept as the composition shell.

## Docs

- [Unified Architecture](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)
- [Agent Integration](docs/agent-integration.md)

---

## 中文

Lociant 把 Android 手机变成一个可被 Agent 通过局域网调用的本地 AI 节点。

它不是完整的 Agent 框架，也不替代 Codex、OpenCode、RikkaHub、Pi 或你自己的自动化系统。Lociant 位于它们下层：提供手机本地模型、摄像头视觉、Android 状态、屏幕工具、通知、存储，以及标准化 API。

## 能力

- 通过 OpenAI-compatible API 和 Ollama 风格接口运行本地 LLM/VLM。
- 提供 MCP Streamable HTTP server，Agent 可以直接接入。
- Android 本地工具：设备状态、剪贴板、打开 App、通知、屏幕文字、屏幕结构、截屏、点击、滑动、返回、主页、最近任务。
- 摄像头视觉：启动视觉 runtime、捕获画面、把图片传给本地 VLM。
- 本地会话和存储，用于保存手机侧上下文。
- 支持 ACP 桌面节点，让手机作为电脑端 Codex 进程的控制台。
- 可见 Android runtime：前台服务、悬浮运行窗口、权限管理、WebView 设置页。

## 快速开始

1. 安装 Android App。
2. 在 App 里启动 Runtime。
3. 确认手机显示局域网地址，例如：

```text
http://10.238.125.4:11434
```

4. 在支持 MCP 的 Agent 客户端里添加：

```text
名称：Lociant
传输：Streamable HTTP
URL：http://<phone-ip>:11434/mcp
Headers：留空
```

如果启用了 API Token，添加：

```text
Authorization: Bearer <token>
```

5. 在当前对话里启用 MCP tools，然后测试：

```text
调用 runtime_status。
调用 device_status。
用 ui_screen_state 获取当前屏幕状态。
启动 vision，再调用 camera_capture。
```

## OpenAI-Compatible API

把手机作为本地模型服务：

```text
Base URL: http://<phone-ip>:11434/v1
Model: 从 GET /v1/models 里选择
API Key: 留空，或按客户端要求填任意非空值
```

## 构建

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

输出 APK：

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

## 边界

Lociant 只负责手机侧能力。桌面 Agent 仍然应该负责规划、工作区文件、Shell、代码编辑和长期任务状态。

更多文档：

- [统一架构](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)
- [Agent 集成](docs/agent-integration.md)
