<div align="center">

# MNNode

### Local OpenAI-compatible AI provider node for agents, scenes, and LAN clients.

[English](#english) · [中文](#中文)

</div>

---

## English

MNNode turns Android phones into local AI provider nodes.

It is an Android-native OpenAI-compatible provider: install the APK, load local models, expose model and tool APIs on LAN, and let agent clients or Scene Packs use the same runtime capabilities.

The product direction is simple: old phones should become useful local AI infrastructure, not e-waste. MNNode should be the local upstream API provider for agents such as RikkaHub, OpenClaw-style runtimes, and custom LAN automation.

## Why MNNode

MNN Chat proves that MNN models can run fast on Android and expose a compatible chat API. MNNode builds the next layer: a phone-sized provider node that exposes local models, vision, runtime state, and device tools through standard HTTP APIs.

The long-term shape is:

```text
Agent / Scene / LAN Client
  -> OpenAI-compatible API
  -> MNNode provider
      -> MNN LLM / VLM
      -> NCNN vision
      -> Android sensors and runtime state
      -> local sessions, events, and tools
```

That makes MNNode less like another chat app and more like a local AI supply layer: cloud-style API outside, Android-native model and device capabilities inside.

## What Works

- **Open model API**: `/v1/chat/completions` and `/api/chat`
- **OpenAI-compatible provider**: upstream API for RikkaHub, OpenClaw-style runtimes, LangChain, and custom clients
- **Tools gateway**: shared local tool registry exposed through OpenAI-style tool calling and direct tool endpoints
- **Local MNN chat**: text chat, image chat, streaming output
- **Serialized request queue**: multiple clients can connect without racing the native runtime
- **Room persistence**: sessions, messages, API requests, runtime events
- **Prompt cache path**: process-local text prompt cache with OpenAI usage metrics
- **Visible runtime service**: foreground service plus Runtime Window for Android-friendly long-running inference
- **Scene Pack system**: WebView-based installable app scenes
- **Native scene runtime**: JSON vision rules can outlive the mounted scene iframe
- **CameraX + NCNN**: local YOLOv8n object detection
- **Model manager**: import, inspect, switch, and delete local models
- **Built-in scenes**: `study-desk`

The model server is core runtime infrastructure, controlled from Settings -> Runtime. It is not packaged or discovered as a Scene Pack.

## Performance Snapshot

MNNode is optimized for Android-native MNN models rather than generic desktop-first runtimes. On the current test phone with `Qwen3.5-2B-MNN`, CPU backend, and 4 threads:

| Path | Decode Speed | First Token | Notes |
|---|---:|---:|---|
| MNNode OpenAI API | ~28-30 tok/s | ~0.2s short prompt, ~0.6-1.1s cached long chat | Streaming, usage, prompt cache |
| MNN Chat reference | ~30+ tok/s | same class | Same MNN runtime family |
| Generic llama.cpp-style CPU path | usually lower on the same phone class | workload dependent | Depends heavily on build, quantization, and thread config |

This is not a universal benchmark claim. It means MNNode's MNN-native path is already in the same performance class as MNN Chat and is a better fit than generic llama.cpp-style CPU serving for this Android runtime target.

## Current Architecture

```text
Scene UI / Runtime Settings
  -> WebView Shell
  -> MNNodeShellBridge for Android UI operations
  -> Local HTTP API for runtime capabilities
  -> MNNodeRuntime
      -> CameraX + NCNN vision
      -> MNN text/image chat
      -> Ktor LAN model API
      -> Room sessions and events
      -> JSON scene rule runtime
  -> JNI / C++ runtimes
```

The app is a **visible runtime node**, not an invisible daemon. Android long-running inference is anchored by a foreground service notification and an optional Runtime Window overlay. The core runtime no longer depends on the visible scene iframe.

## Build

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

APK output:

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

The Android app is currently version `0.3.0`.

## Documentation

- [Unified Architecture](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)

## License

MNNode project code and documentation are licensed under the [Apache License 2.0](LICENSE), unless a file states otherwise.

Third-party runtimes, libraries, and model assets keep their own licenses:

| Component | License / Terms | Role |
|---|---|---|
| Android / AndroidX / CameraX | Apache-2.0 | Android app and camera stack |
| Kotlin | Apache-2.0 | Language and tooling |
| Ktor | Apache-2.0 | Embedded LAN HTTP server |
| Room / SQLite | Apache-2.0 / Public Domain | Local persistence |
| [NCNN](https://github.com/Tencent/ncnn) | BSD-3-Clause | Mobile vision inference |
| [MNN](https://github.com/alibaba/MNN) | Apache-2.0 / BSD-3-Clause components | LLM/VLM runtime |
| YOLOv8 model assets | Ultralytics terms | Object detection model |
| Qwen model assets | Qwen model license / Apache-2.0 where applicable | Text/image model |

Model files are large and may be distributed separately. Check each model's license before redistribution or commercial use.

---

## 中文

MNNode 把 Android 手机变成本地 AI Provider 节点。

它是一个 Android 原生的 OpenAI-compatible Provider：安装 APK，加载本地模型，在局域网暴露模型和工具 API，并让 Agent 客户端或 Scene Pack 使用同一套 runtime 能力。

项目方向很明确：让旧手机成为有用的本地 AI 基础设施，而不是电子垃圾。MNNode 应该成为 RikkaHub、OpenClaw 类 runtime 和自定义局域网自动化的本地上游 API provider。

## 为什么是 MNNode

MNN Chat 证明了 MNN 模型可以在 Android 上高速运行，并暴露兼容 chat API。MNNode 要做的是下一层：把手机变成 provider node，通过标准 HTTP API 暴露本地模型、视觉、runtime 状态和设备工具。

长期形态是：

```text
Agent / Scene / LAN Client
  -> OpenAI-compatible API
  -> MNNode provider
      -> MNN LLM / VLM
      -> NCNN vision
      -> Android sensors and runtime state
      -> local sessions, events, and tools
```

所以 MNNode 不是另一个聊天 App，而更像本地 AI 供应层：外部是云 API 风格，内部是 Android 原生模型与设备能力。

## 已实现能力

- **开放模型 API**：`/v1/chat/completions` 和 `/api/chat`
- **OpenAI-compatible Provider**：作为 RikkaHub、OpenClaw 类 runtime、LangChain 和自定义客户端的上游 API
- **Tools Gateway**：共享本地工具注册表，同时支持 OpenAI 风格 tool calling 和直接工具端点
- **本地 MNN 对话**：文本对话、图片对话、流式输出
- **串行请求队列**：多个客户端可连接，但 native runtime 同一时间只执行一个模型请求
- **Room 持久化**：sessions、messages、API requests、runtime events
- **Prompt cache 路径**：进程内文本 prompt cache，并返回 OpenAI usage 指标
- **可见 runtime service**：前台服务 + Runtime Window，适配 Android 长时间本地推理
- **Scene Pack 系统**：基于 WebView 的可安装应用场景
- **Native 场景 runtime**：JSON 视觉规则可在场景 iframe 关闭后继续持有状态
- **CameraX + NCNN**：本地 YOLOv8n 目标检测
- **模型管理**：导入、查看、切换、删除本地模型
- **内置场景**：`study-desk`

模型服务是核心 runtime 基础设施，由 Settings -> Runtime 控制，不作为 Scene Pack 打包或发现。

## 性能快照

MNNode 优先优化 Android 原生 MNN 模型，而不是桌面优先的通用 runtime。在当前测试手机上，`Qwen3.5-2B-MNN`、CPU backend、4 线程实测：

| 路径 | Decode 速度 | 首 token | 说明 |
|---|---:|---:|---|
| MNNode OpenAI API | 约 28-30 tok/s | 短 prompt 约 0.2s，cache 命中的长对话约 0.6-1.1s | 支持流式、usage、prompt cache |
| MNN Chat reference | 约 30+ tok/s | 同一量级 | 同属 MNN runtime 路线 |
| 通用 llama.cpp-style CPU 路径 | 同级手机上通常更低 | 取决于负载 | 强依赖构建、量化和线程配置 |

这不是跨所有设备和模型的绝对 benchmark。它表达的是：对 MNNode 的 Android runtime 目标来说，MNN-native 路径已经接近 MNN Chat，并且比通用 llama.cpp-style CPU serving 更适合手机端长期运行。

## 当前架构

```text
Scene UI / Runtime Settings
  -> WebView Shell
  -> MNNodeShellBridge for Android UI operations
  -> Local HTTP API for runtime capabilities
  -> MNNodeRuntime
      -> CameraX + NCNN vision
      -> MNN text/image chat
      -> Ktor LAN model API
      -> Room sessions and events
      -> JSON scene rule runtime
  -> JNI / C++ runtimes
```

当前状态是 **visible runtime node**，不是隐藏后台 daemon。Android 长时间推理依赖前台服务通知和可选 Runtime Window 悬浮窗作为可见锚点；核心 runtime 已不依赖当前可见的场景 iframe。

## 构建

```powershell
cd apps/android
.\gradlew.bat :app:assembleDebug
```

APK 输出：

```text
apps/android/app/build/outputs/apk/debug/app-debug.apk
```

当前 Android 应用版本为 `0.3.0`。

## 文档

- [统一架构](docs/unified-architecture.md)
- [OpenAI-Compatible API](docs/openai-compatible.md)

## 许可证

MNNode 项目代码和文档默认使用 [Apache License 2.0](LICENSE)，除非具体文件另有说明。

第三方 runtime、库和模型资产保留各自许可证：

| 组件 | 许可证 / 条款 | 用途 |
|---|---|---|
| Android / AndroidX / CameraX | Apache-2.0 | Android 应用和摄像头栈 |
| Kotlin | Apache-2.0 | 语言和工具链 |
| Ktor | Apache-2.0 | 内嵌局域网 HTTP 服务 |
| Room / SQLite | Apache-2.0 / Public Domain | 本地持久化 |
| [NCNN](https://github.com/Tencent/ncnn) | BSD-3-Clause | 移动端视觉推理 |
| [MNN](https://github.com/alibaba/MNN) | Apache-2.0 / BSD-3-Clause components | LLM/VLM runtime |
| YOLOv8 模型资产 | Ultralytics terms | 目标检测模型 |
| Qwen 模型资产 | Qwen model license / 适用时 Apache-2.0 | 文本/图片模型 |

模型文件体积较大，可能单独分发。重新分发或商业使用前，需要检查每个模型自己的许可证。
