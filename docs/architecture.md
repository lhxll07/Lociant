# MNNode Architecture

> Version: 0.4.1 | Updated: 2026-05-15

[English](#english) · [中文](#中文)

## English

MNNode turns Android phones into local AI runtime nodes. The app has a WebView shell for interaction, but long-running state belongs to the native runtime.

## Shape

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
      -> package and model management
  -> JNI / C++ runtimes
```

`MainActivity` hosts the WebView, permissions, camera preview surface, and Android-only UI concerns. `MNNodeRuntime` owns shared runtime services so the model API can run from the foreground service without depending on the visible scene UI. `MNNodeRuntimeService` owns the foreground notification and Runtime Window overlay.

## Modules

| Area | Path | Role |
|---|---|---|
| Shell UI | `apps/android/app/src/main/assets/web/` | Navigation, settings, model list, scene iframe, alerts. |
| Runtime core | `apps/android/app/src/main/java/com/mnnode/app/runtime/` | Shared runtime instances and foreground service. |
| Bridge | `apps/android/app/src/main/java/com/mnnode/app/MNNodeBridge.kt` | WebView JavaScript API into runtime capabilities. |
| Scene | `apps/android/app/src/main/java/com/mnnode/app/scene/` | Scene discovery, manifests, native rule runtime. |
| Model | `apps/android/app/src/main/java/com/mnnode/app/model/` | Model discovery, import/delete, MNN calls. |
| Server | `apps/android/app/src/main/java/com/mnnode/app/server/` | `/health`, `/v1/models`, `/v1/chat/completions`, `/api/chat`. |
| Session | `apps/android/app/src/main/java/com/mnnode/app/session/` | Room sessions, messages, requests, runtime events. |
| Vision | `apps/android/app/src/main/java/com/mnnode/app/vision/` | Camera frame analysis and NCNN detections. |
| Native | `apps/android/app/src/main/cpp/` | NCNN and MNN JNI bridges. |

## Runtime Rules

Scene rule state lives in `SceneRuntimeManager`, not in iframe timers. A `vision-rules` scene can emit:

- `runtime.snapshot`
- `runtime.state`
- `runtime.event`

This lets scenes such as `baby-guard` keep state and alerts even after the scene iframe is closed.

## Model Server

`model-server` is a built-in runtime panel, not a normal third-party Scene Pack. It controls the LAN model API backed by `ApiServerController` and `ChatController`.

Working surface:

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /api/chat`

The model path supports text chat, image chat, streaming, explicit Room-backed sessions, serialized multi-request inference, and a narrow process-local text prompt cache.

## Visible Runtime

Current state is **visible runtime**, not full headless. Android long-running inference is expected to stay user-visible through:

- foreground service notification
- optional Runtime Window overlay
- battery optimization exemption guidance

The Runtime Window is intentionally small: it shows status, model, and LAN URL; click opens the app, long press hides it, and double click toggles runtime start/stop. This is the practical Android path between a normal app and an unreliable invisible daemon.

Pure headless camera / vision remains out of scope for now; it involves stricter Android lifecycle and vendor-ROM constraints.

---

## 中文

MNNode 把 Android 手机变成本地 AI runtime 节点。App 有 WebView shell 用于交互，但长期运行状态属于 native runtime。

## 结构

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
      -> package and model management
  -> JNI / C++ runtimes
```

`MainActivity` 负责 WebView、权限、摄像头预览 surface 和 Android UI 事务。`MNNodeRuntime` 持有共享 runtime 服务，因此模型 API 可以由前台服务启动，不依赖当前可见的场景 UI。`MNNodeRuntimeService` 持有前台通知和 Runtime Window 悬浮窗。

## 模块

| 区域 | 路径 | 职责 |
|---|---|---|
| Shell UI | `apps/android/app/src/main/assets/web/` | 导航、设置、模型列表、场景 iframe、告警。 |
| Runtime core | `apps/android/app/src/main/java/com/mnnode/app/runtime/` | 共享 runtime 实例和前台服务。 |
| Bridge | `apps/android/app/src/main/java/com/mnnode/app/MNNodeBridge.kt` | WebView JavaScript API。 |
| Scene | `apps/android/app/src/main/java/com/mnnode/app/scene/` | 场景发现、manifest、native 规则 runtime。 |
| Model | `apps/android/app/src/main/java/com/mnnode/app/model/` | 模型发现、导入删除、MNN 调用。 |
| Server | `apps/android/app/src/main/java/com/mnnode/app/server/` | `/health`、`/v1/models`、`/v1/chat/completions`、`/api/chat`。 |
| Session | `apps/android/app/src/main/java/com/mnnode/app/session/` | Room 会话、消息、请求、runtime 事件。 |
| Vision | `apps/android/app/src/main/java/com/mnnode/app/vision/` | 摄像头帧分析和 NCNN 检测。 |
| Native | `apps/android/app/src/main/cpp/` | NCNN 和 MNN JNI 桥。 |

## Runtime 规则

场景规则状态位于 `SceneRuntimeManager`，不依赖 iframe timer。`vision-rules` 场景可以发出：

- `runtime.snapshot`
- `runtime.state`
- `runtime.event`

这样 `baby-guard` 一类场景即使 iframe 关闭，也能保持状态和告警。

## Model Server

`model-server` 是内置 runtime 面板，不是普通第三方 Scene Pack。它控制由 `ApiServerController` 和 `ChatController` 支撑的局域网模型 API。

已可用接口：

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /api/chat`

模型链路支持文本对话、图片对话、流式输出、显式 Room 会话、多 request 串行推理，以及收窄的进程内文本 prompt cache。

## 可见 Runtime

当前是 **visible runtime**，不是完整 headless。Android 长时间本地推理需要保持用户可见：

- 前台服务通知
- 可选 Runtime Window 悬浮窗
- 电池优化豁免引导

Runtime Window 刻意保持很小：显示状态、模型和 LAN URL；点击打开主界面，长按隐藏，双击启停 runtime。这是 Android 上介于普通 App 和不可靠隐藏 daemon 之间的现实路径。

纯 headless camera / vision 暂不做；它们涉及更严格的 Android 生命周期和厂商 ROM 限制。
