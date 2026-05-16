# Unified Architecture

> Version: 0.3 | Updated: 2026-05-17

[English](#english) · [中文](#中文)

## English

MNNode is moving toward an Android-native AI runtime provider. The stable boundary is the local HTTP API, not private WebView bridges or scene-specific native calls.

## Principle

One capability should have one runtime entry point:

- Chat and VLM: `/v1/chat/completions` and `/api/chat`
- Models: `/v1/models`
- Runtime state: `/health` and `/v1/runtime/{command}`
- Storage: `/v1/store/{namespace}/{key}`
- Tools: `/v1/tools` and `/v1/tools/{name}/call`
- Scene loading: `/v1/scenes` and `/v1/scenes/{sceneId}/load`

`MNNodeShellBridge` is deliberately narrow. It stays only for Android UI actions that cannot be represented as normal HTTP calls, such as opening the scene-pack picker or model-package picker.

## Runtime Shape

```text
Scene iframe / LAN client / external agent
  -> HTTP API on Ktor
    -> ApiServerController
      -> ChatController / ToolRegistry / SceneManager / LocalStore / TriggerEngine
        -> MnnRuntime / NcnnRuntime / VisionAnalysisController / Room / Android services
```

The WebView shell is now a client of the same runtime API. Built-in scenes should be pure HTML/JS frontends that call HTTP endpoints directly.

`MainActivity` hosts the WebView, permissions, camera preview surface, and Android-only UI concerns. `MNNodeRuntime` owns shared runtime services so the API can run from the foreground service without depending on the visible scene iframe. `MNNodeRuntimeService` owns the foreground notification and Runtime Window overlay.

## Visible Runtime

MNNode is a visible runtime node, not an invisible daemon. Android long-running inference should stay user-visible through:

- foreground service notification
- optional Runtime Window overlay
- battery optimization guidance

Runtime Window is the practical path between a normal app and unreliable full headless mode. It shows status, model, and LAN URL; click opens the app, long press hides it, and double click toggles runtime start/stop.

Pure headless camera / vision and default boot autostart stay out of scope until the runtime core is stable.

## Tool System

Tools are protocol-neutral native capabilities. OpenAI tool calling, direct `/v1/tools/{name}/call`, and future adapters such as MCP should all map to the same registry.

Current code layout:

| File | Responsibility |
|---|---|
| `server/ToolRegistry.kt` | Registry, manifest generation, call dispatch, error normalization, policy metadata. |
| `server/RuntimeTools.kt` | Runtime and model tools such as status, model listing, preload, and cancel. |
| `server/VisionTools.kt` | Camera / vision runtime tools. These require the interactive Activity. |
| `server/StorageTools.kt` | Persistent event and key-value tools. |
| `server/NotificationTools.kt` | Local notification and webhook tools. |

Every tool definition carries policy metadata:

```json
{
  "x_execution": "local",
  "x_policy": {
    "local": true,
    "remoteAllowed": true,
    "requiresActivity": false,
    "sideEffect": true
  }
}
```

This keeps the implementation simple while leaving room for auth, MCP exposure, and stricter LAN policy later.

## Scene Packs

Scene Packs are app-level experiences, not runtime services. They should not own model-server lifecycle and should not add private bridge methods.

Minimal package shape:

```text
my-scene/
  manifest.json
  web/
    index.html
```

Installable zip contents must contain `manifest.json` and `web/index.html` directly.

```powershell
tar -a -cf dist/scenes/my-scene.scene.zip -C scenes/my-scene manifest.json web/index.html
```

Scene-side calls should use:

```javascript
await fetch('/v1/tools/start_vision_rules/call', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ arguments: { modelId: 'yolov8n' } })
})
```

For model calls:

```javascript
await fetch('/v1/chat/completions', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    model: 'qwen3.5-2b-mnn',
    messages: [{ role: 'user', content: 'What is visible?' }],
    stream: false
  })
})
```

PostMessage remains useful for shell lifecycle messages such as `scene.ready`, `runtime.subscribe`, `runtime.command`, locale broadcasts, and iframe disposal.

Use native runtime rules only for long-running behavior that must outlive the iframe. Keep UI-only resources disposable.

## OpenAI Tool Compatibility

MNNode supports the OpenAI-compatible tool schema in chat requests. The conservative default is:

- forced `tool_choice` returns `finish_reason: "tool_calls"` unless local execution is explicitly requested
- `execute_tools: true` lets MNNode execute the selected local tool and feed the result back to the model
- `tool_choice: "auto"` remains conservative until local models are reliable enough to choose tools autonomously

This makes MNNode usable as an upstream local API provider for clients such as OpenClaw-style agents without requiring those clients to know Android internals.

## Current Boundaries

Keep:

- `MNNodeShellBridge`: Android UI only
- `ToolRegistry`: protocol-neutral capability registry
- Ktor routes: public runtime API
- `TriggerEngine`: continuous scene rules

Avoid:

- scene-specific native bridge calls
- duplicated model/vision/storage paths
- treating runtime services as Scene Packs
- adding compatibility layers for removed scene-server or retired built-in scene paths

## Next Architecture Work

1. Harden `/v1/tools` policy with optional auth and remote visibility rules.
2. Move more runtime diagnostics into structured API responses.
3. Keep `study-desk` as the only built-in scene until the scene model is stable.
4. Expose model capability metadata for text/image/stream/context/cache.
5. Add embeddings later as `/v1/embeddings`, backed by MNN embedding support.
6. Add MCP only as an adapter over `ToolRegistry`, not as a second capability system.

## 中文

MNNode 正在走向 Android 原生 AI runtime provider。稳定边界是本地 HTTP API，而不是私有 WebView bridge 或场景专用 native 调用。

## 原则

一个能力只保留一个 runtime 入口：

- Chat / VLM：`/v1/chat/completions` 和 `/api/chat`
- 模型：`/v1/models`
- Runtime 状态：`/health` 和 `/v1/runtime/{command}`
- 存储：`/v1/store/{namespace}/{key}`
- 工具：`/v1/tools` 和 `/v1/tools/{name}/call`
- 场景加载：`/v1/scenes` 和 `/v1/scenes/{sceneId}/load`

`MNNodeShellBridge` 必须保持很窄，只用于文件选择、模型包导入这类不能自然表示为 HTTP 调用的 Android UI 操作。

## Runtime 形态

```text
Scene iframe / LAN client / external agent
  -> HTTP API on Ktor
    -> ApiServerController
      -> ChatController / ToolRegistry / SceneManager / LocalStore / TriggerEngine
        -> MnnRuntime / NcnnRuntime / VisionAnalysisController / Room / Android services
```

WebView shell 现在也是同一套 runtime API 的客户端。内置场景应该是纯 HTML/JS frontend，直接调用 HTTP endpoint。

`MainActivity` 负责 WebView、权限、摄像头预览 surface 和 Android UI 事务。`MNNodeRuntime` 持有共享 runtime 服务，因此 API 可以由前台服务运行，不依赖当前可见的 scene iframe。`MNNodeRuntimeService` 持有前台通知和 Runtime Window 悬浮窗。

## 可见 Runtime

MNNode 是可见 runtime node，不是隐藏 daemon。Android 长时间本地推理应该保持用户可见：

- 前台服务通知
- 可选 Runtime Window 悬浮窗
- 电池优化豁免引导

Runtime Window 是普通 App 和不可靠完整 headless 之间的现实路径。它显示状态、模型和 LAN URL；点击打开 App，长按隐藏，双击启停 runtime。

纯 headless camera / vision 和默认开机自启暂不做，等 runtime core 稳定后再考虑。

## 工具系统

Tools 是协议无关的 native 能力。OpenAI tool calling、直接 `/v1/tools/{name}/call`，以及未来 MCP adapter，都应该映射到同一套 registry。

当前代码结构：

| 文件 | 职责 |
|---|---|
| `server/ToolRegistry.kt` | 注册表、manifest、调用分发、错误归一化、策略元数据。 |
| `server/RuntimeTools.kt` | runtime 和模型工具，例如状态、模型列表、preload、cancel。 |
| `server/VisionTools.kt` | camera / vision runtime 工具，需要交互式 Activity。 |
| `server/StorageTools.kt` | 持久事件和 key-value 工具。 |
| `server/NotificationTools.kt` | 本地通知和 webhook 工具。 |

每个工具定义都带策略元数据：

```json
{
  "x_execution": "local",
  "x_policy": {
    "local": true,
    "remoteAllowed": true,
    "requiresActivity": false,
    "sideEffect": true
  }
}
```

这让当前实现保持简单，同时为后续 auth、MCP 暴露和更严格的局域网策略留出空间。

## Scene Pack

Scene Pack 是应用层体验，不是 runtime 服务。场景不拥有 model-server 生命周期，也不新增私有 bridge 方法。

最小包结构：

```text
my-scene/
  manifest.json
  web/
    index.html
```

可安装 zip 必须直接包含 `manifest.json` 和 `web/index.html`。

```powershell
tar -a -cf dist/scenes/my-scene.scene.zip -C scenes/my-scene manifest.json web/index.html
```

场景侧能力调用使用 HTTP API：

```javascript
await fetch('/v1/tools/start_vision_rules/call', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ arguments: { modelId: 'yolov8n' } })
})
```

模型调用使用：

```javascript
await fetch('/v1/chat/completions', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    model: 'qwen3.5-2b-mnn',
    messages: [{ role: 'user', content: 'What is visible?' }],
    stream: false
  })
})
```

PostMessage 只保留 shell 生命周期用途，例如 `scene.ready`、`runtime.subscribe`、`runtime.command`、语言广播和 iframe dispose。

只有需要在 iframe 卸载后继续运行的行为，才放进 native runtime rules。UI-only 资源应当随场景卸载。

## OpenAI Tool 兼容

MNNode 支持 OpenAI-compatible tool schema。保守默认行为是：

- 强制 `tool_choice` 默认返回 `finish_reason: "tool_calls"`，除非显式请求本地执行
- `execute_tools: true` 让 MNNode 执行本地工具并把结果回填给模型
- `tool_choice: "auto"` 暂时保持保守，直到本地小模型足够可靠地自主选择工具

这让 MNNode 可以作为 OpenClaw 类 agent 的本地上游 API provider，而不要求客户端理解 Android 内部实现。

## 当前边界

保留：

- `MNNodeShellBridge`：只做 Android UI
- `ToolRegistry`：协议无关能力注册表
- Ktor routes：公开 runtime API
- `TriggerEngine`：连续场景规则

避免：

- 场景专用 native bridge 调用
- 重复的模型 / 视觉 / 存储路径
- 把 runtime 服务当作 Scene Pack
- 给已删除的旧 scene-server 或旧内置场景路径增加兼容层

## 下一步架构工作

1. 用可选 auth 和 remote visibility 规则加固 `/v1/tools` policy。
2. 把更多 runtime diagnostics 放进结构化 API 响应。
3. 在 scene 模型稳定前，只保留 `study-desk` 作为内置场景。
4. 暴露 text/image/stream/context/cache 等模型能力元数据。
5. 后续用 MNN embedding 支持实现 `/v1/embeddings`。
6. MCP 只作为 `ToolRegistry` 的 adapter，不作为第二套能力系统。
