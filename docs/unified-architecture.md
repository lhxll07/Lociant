# Unified Architecture

> Version: 0.4 | Updated: 2026-05-18

[English](#english) | [中文](#中文)

## English

Lociant is an Android-native capability provider for AI agents. The stable boundary is the local HTTP API, not private WebView bridges, scene-specific native calls, or a desktop-agent loop embedded in the phone app.

## Principle

One capability should have one runtime entry point:

| Capability | Entry point |
|---|---|
| Chat / VLM | `/v1/chat/completions`, `/api/chat` |
| Models | `/v1/models` |
| Runtime state | `/health`, runtime commands |
| Tools | `/v1/tools`, `/v1/tools/{name}/call` |
| Storage | `/v1/store/{namespace}/{key}` |
| Scenes | `/v1/scenes`, `/v1/scenes/{sceneId}/load` |

Everything user-facing should become either a client of these APIs or a narrow Android UI action. Runtime services should not be hidden inside Scene Packs.

## Runtime Shape

```text
Scene iframe / LAN client / agent client
  -> Ktor HTTP API
    -> ApiServerController
      -> ChatController / ToolRegistry / SceneManager / LocalStore / TriggerEngine
        -> MnnRuntime / NcnnRuntime / VisionAnalysisController / Room / Android services
```

`MainActivity` owns WebView, permissions, camera preview surface, and Android UI concerns.

`MNNodeRuntime` and `MNNodeRuntimeService` are still internal implementation names. They own shared runtime singletons, foreground service lifecycle, and the Runtime Window overlay. They do not define the public product name.

`MNNodeShellBridge` remains deliberately narrow. It is only for Android UI actions that do not map cleanly to HTTP, such as opening package pickers or permission screens.

## Agent Boundary

Lociant sits below agent systems:

```text
Pi / OpenClaw / OpenCode / RikkaHub / custom agent
  -> planning, workspace tools, shell, edit, read, grep, UI
  -> OpenAI-compatible model and tool calls
  -> Lociant
      -> local LLM / VLM
      -> camera and vision
      -> Android notifications and runtime state
      -> local sessions and events
```

Keep these out of Lociant:

- PC workspace file access
- shell execution for desktop projects
- coding-agent memory and skill orchestration
- multi-step task planning
- vendor-specific agent compatibility hacks

Lociant may receive client-owned OpenAI tool definitions and return standard `tool_calls`. The client should execute those tools unless the selected tool is a real Lociant local tool and the request explicitly asks Lociant to execute it.

## Visible Runtime

Lociant is a visible phone runtime, not an invisible daemon. Android background execution is vendor-sensitive, so stable local inference should be user-visible:

- foreground service notification
- Runtime Window overlay
- clear Runtime settings
- battery optimization guidance

Locked-screen and fully headless behavior remain best-effort system behavior, not a core guarantee.

## Tool System

Tools are protocol-neutral phone capabilities. OpenAI tool calling, direct HTTP tool calls, and future adapters such as MCP should all map to the same `ToolRegistry`.

Current local tools:

| Tool | Purpose |
|---|---|
| `runtime_status` | API/runtime status |
| `runtime_resources` | Android package and resource info |
| `model_list` | Installed and built-in models |
| `model_preload` | Queue model preload |
| `inference_cancel` | Cancel current inference |
| `vision_status` | Camera/vision runtime status |
| `vision_start` | Start continuous camera vision analysis |
| `camera_capture` | Capture the latest camera frame as a JPEG data URL |
| `vision_stop` | Stop continuous camera vision analysis |
| `event_record` | Persist a runtime event |
| `store_increment` | Increment a numeric local-store value |
| `notification_post` | Send Android notification |
| `webhook_post` | Queue JSON webhook POST |

Tool definitions carry policy metadata so LAN visibility, auth, side effects, and Activity requirements can be hardened later without inventing another capability system.

## Scene Packs

Scene Packs are app-level experiences. They should not own model-server lifecycle and should not add private native bridge methods.

Scene code should call the same HTTP API used by LAN clients:

```javascript
await fetch('/v1/tools/vision_start/call', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ arguments: { modelId: 'yolov8n' } })
})
```

Scene Packs remain useful for phone-local UX. They are not the runtime boundary.

## Model Runtime

MNN is the current LLM/VLM backend. NCNN is the current vision backend. Both are implementation details below the HTTP capability layer.

Model packages should be inferred from `config.json` and known MNN files such as `llm.mnn`, `llm.mnn.weight`, `tokenizer.txt`, and `llm_config.json`. `visual.mnn` is optional because pure LLM models should import cleanly.

## Compatibility

Public compatibility surfaces should remain stable:

- OpenAI-compatible request/response shape
- Ollama-compatible `/api/chat`
- local tool names
- `/v1/models`
- `X-MNNode-Session-Id` while existing clients use it
- `mnnode` diagnostics field while existing clients and scripts consume it

The product name can be Lociant while old internal identifiers remain until there is a strong reason to rename them.

## Next Work

1. Harden LAN exposure with optional auth and remote tool visibility rules.
2. Keep model import and model-market metadata config-driven.
3. Improve Runtime Window diagnostics and recovery.
4. Keep `study-desk` as the only built-in Scene Pack until the scene model proves stable.
5. Add MCP only as an adapter over `ToolRegistry`, not as a second tool system.

---

## 中文

Lociant 是面向 AI agent 的 Android 原生能力 provider。稳定边界是本地 HTTP API，而不是私有 WebView bridge、场景专用 native 调用，或嵌在手机 App 里的桌面 agent loop。

## 原则

一个能力只保留一个 runtime 入口：

| 能力 | 入口 |
|---|---|
| Chat / VLM | `/v1/chat/completions`, `/api/chat` |
| Models | `/v1/models` |
| Runtime state | `/health`, runtime commands |
| Tools | `/v1/tools`, `/v1/tools/{name}/call` |
| Storage | `/v1/store/{namespace}/{key}` |
| Scenes | `/v1/scenes`, `/v1/scenes/{sceneId}/load` |

所有面向用户的功能都应该成为这些 API 的客户端，或者是很窄的 Android UI 操作。Runtime 服务不应该藏在 Scene Pack 里。

## Runtime 形态

```text
Scene iframe / LAN client / agent client
  -> Ktor HTTP API
    -> ApiServerController
      -> ChatController / ToolRegistry / SceneManager / LocalStore / TriggerEngine
        -> MnnRuntime / NcnnRuntime / VisionAnalysisController / Room / Android services
```

`MainActivity` 负责 WebView、权限、摄像头预览 surface 和 Android UI 事务。

`MNNodeRuntime` 和 `MNNodeRuntimeService` 仍然是内部实现名。它们持有共享 runtime 单例、前台服务生命周期和 Runtime Window 悬浮窗，不定义公开产品名。

`MNNodeShellBridge` 必须保持很窄，只用于无法自然表示成 HTTP 调用的 Android UI 行为，例如打开包选择器或权限页面。

## Agent 边界

Lociant 位于 agent 系统下层：

```text
Pi / OpenClaw / OpenCode / RikkaHub / custom agent
  -> planning, workspace tools, shell, edit, read, grep, UI
  -> OpenAI-compatible model and tool calls
  -> Lociant
      -> local LLM / VLM
      -> camera and vision
      -> Android notifications and runtime state
      -> local sessions and events
```

这些职责不放进 Lociant：

- PC 工作区文件访问
- 桌面项目的 shell 执行
- coding-agent memory 和 skill orchestration
- 多步任务规划
- 针对某个 agent 客户端的特殊兼容 hack

Lociant 可以接收客户端自有的 OpenAI tool definitions，并返回标准 `tool_calls`。除非选中的工具是真实的 Lociant 本地工具，并且请求显式要求 Lociant 执行，否则工具应该由客户端执行。

## 可见 Runtime

Lociant 是可见手机 runtime，不是隐藏 daemon。Android 后台执行高度依赖厂商策略，所以稳定的本地推理应该保持用户可见：

- 前台服务通知
- Runtime Window 悬浮窗
- 清晰的 Runtime 设置
- 电池优化引导

锁屏和完全 headless 行为只能视为系统层面的 best-effort，不作为核心保证。

## 工具系统

Tools 是协议无关的手机能力。OpenAI tool calling、直接 HTTP tool call，以及未来 MCP adapter，都应该映射到同一个 `ToolRegistry`。

当前本地工具：

| Tool | 用途 |
|---|---|
| `runtime_status` | API/runtime 状态 |
| `runtime_resources` | Android 包和资源信息 |
| `model_list` | 已安装和内置模型 |
| `model_preload` | 排队预加载模型 |
| `inference_cancel` | 取消当前推理 |
| `vision_status` | 摄像头/视觉 runtime 状态 |
| `vision_start` | 启动连续摄像头视觉分析 |
| `camera_capture` | 将最新摄像头画面捕获为 JPEG data URL |
| `vision_stop` | 停止连续摄像头视觉分析 |
| `event_record` | 持久化 runtime 事件 |
| `store_increment` | 递增本地存储里的数值 |
| `notification_post` | 发送 Android 通知 |
| `webhook_post` | 排队发送 JSON webhook |

工具定义带策略元数据，后续可以在不引入第二套能力系统的前提下加固 LAN 可见性、auth、副作用和 Activity 依赖规则。

## Scene Packs

Scene Pack 是应用层体验，不拥有 model-server 生命周期，也不新增私有 native bridge 方法。

场景代码应该调用和 LAN 客户端相同的 HTTP API：

```javascript
await fetch('/v1/tools/vision_start/call', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ arguments: { modelId: 'yolov8n' } })
})
```

Scene Pack 对手机本地 UX 仍然有价值，但它不是 runtime 边界。

## 模型 Runtime

MNN 是当前 LLM/VLM 后端。NCNN 是当前视觉后端。两者都是 HTTP capability layer 下面的实现细节。

模型包应该从 `config.json` 和已知 MNN 文件自动推断，例如 `llm.mnn`、`llm.mnn.weight`、`tokenizer.txt` 和 `llm_config.json`。`visual.mnn` 是可选的，因为纯 LLM 模型也应该能正常导入。

## 兼容性

公开兼容面应保持稳定：

- OpenAI-compatible request/response shape
- Ollama-compatible `/api/chat`
- 本地工具名
- `/v1/models`
- 现有客户端仍使用时保留 `X-MNNode-Session-Id`
- 现有客户端和脚本仍使用时保留 `mnnode` diagnostics 字段

公开产品名可以是 Lociant；旧内部标识符只有在有明确收益时才需要重命名。

## 下一步

1. 用可选 auth 和 remote tool visibility 规则加固 LAN 暴露。
2. 保持模型导入和模型市场元数据由 config 驱动。
3. 改进 Runtime Window 诊断和恢复能力。
4. 在 scene 模型稳定前，只保留 `study-desk` 作为内置 Scene Pack。
5. MCP 只作为 `ToolRegistry` 的 adapter，不作为第二套工具系统。
