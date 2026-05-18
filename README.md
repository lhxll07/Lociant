<div align="center">

# Lociant

### A physical-world interface for AI agents.

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android-3DDC84.svg)](apps/android)
[![OpenAI Compatible](https://img.shields.io/badge/OpenAI-compatible-111827.svg)](docs/openai-compatible.md)
[![Local First](https://img.shields.io/badge/local-first-0F766E.svg)](docs/unified-architecture.md)

[English](#english) | [中文](#中文)

</div>

---

## English

AI agents can reason, code, search, and use tools.

But they still do not know what is happening in the room.

**Lociant turns the Android phone you already own into local eyes, memory, alerts, and on-device intelligence for the agents you already use.**

Put a spare phone on a desk, shelf, counter, or doorway. It becomes a private real-world interface that your PC agent, automation script, or chat client can call over the local network.

What that means in practice:

- 👁️ your agent can see a desk, room, pet, person, or scene
- 🔔 it can notify you when something changes
- 🧠 it can run local chat and vision models on the phone
- 🌐 it works with OpenAI-compatible clients over LAN
- 🪟 it stays visible and controllable through Android foreground runtime
- 📦 it can import and download models directly on device

Under the hood, Lociant uses an Android app with a foreground runtime, local HTTP API, MNN model inference, CameraX camera access, NCNN vision, local sessions, storage, and tool calling. The point is not to replace your agent. The point is to give it a local body.

Zero-friction agent setup:

1. Install the Android app and start Runtime.
2. Add an MCP server in your agent client:

```text
Name: Lociant
Transport: Streamable HTTP
URL: http://<phone-ip>:11434/mcp
Headers: empty
```

3. Enable the MCP tools in the current chat.
4. Ask: `Call runtime_status`, or `Start vision and capture a photo`.

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

[Architecture](docs/unified-architecture.md) · [OpenAI API](docs/openai-compatible.md) · [Agent Integration](docs/agent-integration.md)

---

## 中文

AI agent 已经会推理、写代码、搜索和调用工具。

但它们仍然不知道房间里正在发生什么。

**Lociant 把你已经拥有的 Android 手机，变成你正在使用的 agent 的本地眼睛、记忆、提醒和端侧智能。**

把一台闲置手机放在桌上、架子上、柜台边或门口。它就会成为一个私有的现实世界接口，让你的电脑 agent、自动化脚本或聊天客户端可以在局域网内调用。

这意味着：

- 👁️ agent 可以看到书桌、房间、宠物、人物或场景
- 🔔 现实发生变化时，它可以提醒你
- 🧠 手机可以运行本地聊天和视觉模型
- 🌐 现有 OpenAI-compatible 客户端可以通过局域网连接
- 🪟 Android 前台 runtime 让它保持可见、可控
- 📦 可以直接在手机上导入和下载模型

底层实现上，Lociant 是一个 Android App：有前台 runtime、本地 HTTP API、MNN 模型推理、CameraX 摄像头、NCNN 视觉、本地会话、存储和工具调用。它不是要替代你的 agent，而是给 agent 一个本地身体。

零成本接入 agent：

1. 安装 Android App，并启动 Runtime。
2. 在 agent 客户端添加 MCP server：

```text
名称：Lociant
传输类型：Streamable HTTP
服务器地址：http://<phone-ip>:11434/mcp
请求头：留空
```

3. 在当前对话里启用 MCP 工具。
4. 直接问：`调用 runtime_status`，或 `启动视觉并拍一张照片`。

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

[架构](docs/unified-architecture.md) · [OpenAI API](docs/openai-compatible.md) · [Agent 集成](docs/agent-integration.md)
