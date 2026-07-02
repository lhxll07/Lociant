# Unified Architecture

 > Version: 0.5 | Updated: 2026-07-03

[English](#english) | [中文](#中文)

## English

Lociant is an Android-native capability provider for AI agents. The stable boundary is the local HTTP API, not private WebView bridges, scene-specific native calls, or a desktop-agent loop embedded in the phone app.

## Principle

One capability should have one runtime entry point:

| Capability | Entry point |
|---|---|
 | Chat / VLM | `/v1/chat/completions`, `/api/chat` |
 | Models | `/v1/models` |
 | Sessions | `/v1/sessions` |
 | Runtime state | `/health`, `/v1/runtime/{command}` |
 | Tools | `/v1/tools`, `/v1/tools/{name}/call` |
 | Chat status | `/v1/chat/status/{requestId}`, `/v1/chat/queue` |
 | MCP | `/mcp` |

Everything user-facing should become either a client of these APIs or a narrow Android UI action. Runtime services should not be hidden inside app-level scene code.

## Runtime Shape

```text
Scene iframe / LAN client / agent client
  -> Ktor HTTP API
    -> ApiServerController
      -> ChatController / ToolRegistry / LocalStore
        -> MnnRuntime / NcnnRuntime / VisionAnalysisController / Room / Android services
```

`MainActivity` owns WebView, permission entry points, and Android UI concerns. CameraX vision belongs to `:local-runtime` and is attached by the foreground runtime service when needed.

The Android project is split by runtime capability rather than by UI page:
 `:core` keeps protocol-neutral contracts, `:data` owns persistence, `:local-runtime` owns MNN/NCNN and CameraX vision, `:phone-tools` owns Android device tools, `:mcp` is the MCP protocol adapter, and `:app` is the composition shell.

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
      -> Android screen context and runtime state
      -> explicit Android UI actions
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
- in-app WebView UI (Settings page) for runtime, server, vision, and tool exposure settings
- battery optimization guidance

Locked-screen and fully headless behavior remain best-effort system behavior, not a core guarantee.

The in-app WebView UI (`assets/web/index.html`) is the primary UX surface for configuring server port, API Token, tool exposure level, default model, vision, and runtime window behavior.

The WebView UI source lives in `apps/android/app/src/main/web-src/` and compiles into the `assets/web/` directory. App icons live under the Android resource tree.

## Tool System

Tools are protocol-neutral phone capabilities. OpenAI tool calling, direct HTTP tool calls, and MCP all map to the same `ToolRegistry`.

Current local tools:

 | Tool | Purpose |
 |---|---|
 | `runtime_status` | API/runtime status |
 | `model_list` | Installed and built-in models |
 | `llm_status` | Phone-local LLM readiness and available chat models |
 | `llm_chat` | Ask the phone-local LLM through MCP or direct tool calls |
 | `vision_status` | Camera/vision runtime status |
 | `vision_start` | Start continuous camera vision analysis |
 | `camera_capture` | Capture the latest camera frame as a JPEG data URL |
 | `vision_stop` | Stop continuous camera vision analysis |
 | `device_status` | Battery, network, screen, and permission state |
 | `clipboard_read` | Read Android clipboard text when Android allows it |
 | `clipboard_write` | Write Android clipboard text |
 | `app_open` | Open an installed app or safe deep link |
 | `ui_screen_state` | One-call screen context with device state, UI text, actionable `nodeId` values, and optional screenshot |
 | `ui_click_node` | Click a `nodeId` returned by `ui_screen_state` |
 | `ui_tap` | Tap at screen coordinates |
 | `ui_swipe` | Swipe between two screen coordinates |
 | `ui_back` | Press the Android Back button |
 | `ui_home` | Press the Android Home button |
 | `ui_recent_apps` | Open the Android recent apps overview |
 | `ui_notifications` | Open the Android notification shade |
 | `ui_quick_settings` | Open Android quick settings |
 | `ui_wait` | Wait for a fixed duration, UI idle, or visible text after an action |

 Tool definitions carry policy metadata so LAN visibility, auth, side effects, and Activity requirements can be enforced without inventing another capability system. Runtime settings expose a small remote visibility policy: `read`, `sensor`, or `action`.
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

1. Keep LAN auth and remote tool visibility rules simple enough for normal users to understand. (The in-app WebView UI already exposes API Token and tool exposure level settings.)
 2. Keep model import and model-market metadata config-driven.
 3. Improve Runtime Window diagnostics and recovery.
4. Keep MCP policy as a thin projection of `ToolRegistry`, not a second tool system.

---

## 中文

Lociant 是面向 AI agent 的 Android 原生能力 provider。稳定边界是本地 HTTP API，而不是私有 WebView bridge、场景专用 native 调用，或嵌在手机 App 里的桌面 agent loop。

## 原则

一个能力只保留一个 runtime 入口：

 | 能力 | 入口 |
 |---|---|
 | Chat / VLM | `/v1/chat/completions`, `/api/chat` |
 | Models | `/v1/models` |
 | Sessions | `/v1/sessions` |
 | Runtime state | `/health`, `/v1/runtime/{command}` |
 | Tools | `/v1/tools`, `/v1/tools/{name}/call` |
 | Chat status | `/v1/chat/status/{requestId}`, `/v1/chat/queue` |
 | MCP | `/mcp` |

所有面向用户的功能都应该成为这些 API 的客户端，或者是很窄的 Android UI 操作。Runtime 服务不应该藏在应用层场景代码里。

## Runtime 形态

```text
Scene iframe / LAN client / agent client
  -> Ktor HTTP API
    -> ApiServerController
      -> ChatController / ToolRegistry / LocalStore
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
      -> Android screen context and runtime state
      -> explicit Android UI actions
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
- App 内 WebView UI（设置页）用于 runtime、server、vision 和工具暴露策略配置
- 电池优化引导

锁屏和完全 headless 行为只能视为系统层面的 best-effort，不作为核心保证。

App 内 WebView UI（`assets/web/index.html`）是配置端口、API Token、工具暴露级别、默认模型、视觉和悬浮窗行为的主要 UX 界面。

WebView UI 源码在 `apps/android/app/src/main/web-src/`，构建后输出到 `assets/web/`。App 图标保存在 Android resource tree 中。

## 工具系统

Tools 是协议无关的手机能力。OpenAI tool calling、直接 HTTP tool call 和 MCP 都映射到同一个 `ToolRegistry`。

当前本地工具：

| Tool | 用途 |
|---|---|
| `runtime_status` | API/runtime 状态 |
| `model_list` | 已安装和内置模型 |
| `llm_status` | 手机本地 LLM 就绪状态和可用的聊天模型 |
| `llm_chat` | 通过 MCP 或直接工具调用询问手机本地 LLM |
| `vision_status` | 摄像头/视觉 runtime 状态 |
| `vision_start` | 启动连续摄像头视觉分析 |
| `camera_capture` | 将最新摄像头画面捕获为 JPEG data URL |
| `vision_stop` | 停止连续摄像头视觉分析 |
 | `device_status` | 电量、网络、屏幕和权限状态 |
 | `clipboard_read` | 在系统允许时读取 Android 剪贴板文本 |
 | `clipboard_write` | 写入 Android 剪贴板文本 |
 | `app_open` | 打开已安装应用或安全 deep link |
 | `ui_screen_state` | 一次性读取屏幕上下文、设备状态、可操作 `nodeId` 和可选截图 |
 | `ui_click_node` | 点击 `ui_screen_state` 返回的 `nodeId` |
 | `ui_tap` | 在屏幕坐标处点击 |
 | `ui_swipe` | 在两个屏幕坐标之间滑动 |
 | `ui_back` | 按 Android 返回键 |
 | `ui_home` | 按 Android Home 键 |
 | `ui_recent_apps` | 打开 Android 最近任务视图 |
 | `ui_notifications` | 打开 Android 通知栏 |
 | `ui_quick_settings` | 打开 Android 快捷设置 |
 | `ui_wait` | 动作后等待 UI 空闲或等待文字出现 |

工具定义带策略元数据，用于执行 LAN 可见性、auth、副作用和 Activity 依赖规则，而不引入第二套能力系统。Runtime 设置提供一个很小的远程可见性策略：`read`、`sensor` 或 `action`。
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

1. 保持 LAN auth 和远程工具可见性规则足够简单，让普通用户能理解。（App 内 WebView UI 已提供 API Token 和工具暴露级别的设置入口。）
 2. 保持模型导入和模型市场元数据由 config 驱动。
 3. 改进 Runtime Window 诊断和恢复能力。
4. 让 MCP policy 始终只是 `ToolRegistry` 的薄投影，不成为第二套工具系统。
