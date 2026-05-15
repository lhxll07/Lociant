<div align="center">

# MNNode

### Phone-side AI runtime for local models, scenes, and LAN APIs.

[English](#english) · [中文](#中文)

</div>

---

## English

MNNode turns Android phones into local AI runtime nodes.

It is an Android-native, Ollama-like edge node: install the APK, load local models, expose OpenAI/Ollama-style APIs on LAN, and let Scene Packs use the same runtime capabilities.

The product direction is simple: old phones should become useful local AI infrastructure, not e-waste.

## What Works

- **Open model API**: `/v1/chat/completions` and `/api/chat`
- **Local MNN chat**: text chat, image chat, streaming output
- **Serialized request queue**: multiple clients can connect without racing the native runtime
- **Room persistence**: sessions, messages, API requests, runtime events
- **Prompt cache path**: first process-local cache for explicit text sessions
- **Visible runtime service**: foreground service plus Runtime Window for Android-friendly long-running inference
- **Scene Pack system**: WebView-based installable scenes
- **Native scene runtime**: JSON vision rules can outlive the mounted scene iframe
- **CameraX + NCNN**: local YOLOv8n object detection
- **Model manager**: import, inspect, switch, and delete local models
- **Built-in panels/scenes**: `model-server`, `baby-guard`, `study-desk`

`model-server` is a built-in runtime panel. It is not a normal third-party Scene Pack; it controls core runtime infrastructure.

## Current Architecture

```text
Scene / Runtime Panel UI
  -> WebView Shell
  -> MNNodeBridge
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

- [Architecture](docs/architecture.md)
- [Model Server Runtime Panel](docs/model-server.md)
- [Scene Pack Guide](docs/scene-pack.md)
- [Future Direction](docs/future.md)

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

MNNode 把 Android 手机变成本地 AI runtime 节点。

它是一个 Android 原生的手机端 Ollama-like 节点：安装 APK，加载本地模型，在局域网暴露 OpenAI/Ollama 风格 API，并让 Scene Pack 使用同一套 runtime 能力。

项目方向很明确：让旧手机成为有用的本地 AI 基础设施，而不是电子垃圾。

## 已实现能力

- **开放模型 API**：`/v1/chat/completions` 和 `/api/chat`
- **本地 MNN 对话**：文本对话、图片对话、流式输出
- **串行请求队列**：多个客户端可连接，但 native runtime 同一时间只执行一个模型请求
- **Room 持久化**：sessions、messages、API requests、runtime events
- **Prompt cache 路径**：首版显式文本 session 的进程内 cache
- **可见 runtime service**：前台服务 + Runtime Window，适配 Android 长时间本地推理
- **Scene Pack 系统**：基于 WebView 的可安装场景
- **Native 场景 runtime**：JSON 视觉规则可在场景 iframe 关闭后继续持有状态
- **CameraX + NCNN**：本地 YOLOv8n 目标检测
- **模型管理**：导入、查看、切换、删除本地模型
- **内置面板/场景**：`model-server`、`baby-guard`、`study-desk`

`model-server` 是内置 runtime 面板，不是普通第三方 Scene Pack；它控制核心 runtime 基础设施。

## 当前架构

```text
Scene / Runtime Panel UI
  -> WebView Shell
  -> MNNodeBridge
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

- [架构总览](docs/architecture.md)
- [Model Server Runtime Panel](docs/model-server.md)
- [Scene Pack 开发指南](docs/scene-pack.md)
- [未来方向](docs/future.md)

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
