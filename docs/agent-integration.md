# Agent Integration

 > Version: 0.3 | Updated: 2026-07-03

[English](#english) | [中文](#中文)

## English

Lociant is a phone-side capability provider for agent systems. It is not an agent harness.

## Division Of Responsibility

```text
Agent client
  -> planning
  -> workspace tools
  -> shell / edit / read / grep
  -> task state and UI
  -> OpenAI-compatible calls to Lociant

Lociant on Android
  -> local LLM / VLM
  -> camera / vision
  -> sensors and runtime state
  -> Android screen context
  -> explicit Android UI actions
```

This division keeps the phone useful without asking it to behave like a desktop coding workstation.

## Quick Start

1. Install Lociant on the Android phone and start Runtime.
2. Confirm the phone shows a LAN URL such as:

```text
http://10.238.125.4:11434
```

3. Add an MCP server in the agent client:

```text
Name: Lociant
Transport: Streamable HTTP
URL: http://<phone-ip>:11434/mcp
Headers: empty
```

If API Token is enabled in Runtime settings, add:

```text
Authorization: Bearer <token>
```

4. Enable the MCP server/tools in the current chat.
5. Test with:

```text
Call runtime_status.
Start vision, then call camera_capture.
```

Adding the MCP server only installs the tool source. Most clients still require enabling those tools in each chat or assistant profile.

Runtime settings can limit exposed tools. Open the in-app WebView UI (Settings → Capabilities → Remote Tools) to configure:

| Level | Exposes |
|---|---|
| `read` | runtime/model status |
| `sensor` | read tools plus camera/vision tools |
| `action` | all Android-native action tools, including app launch, clipboard writes, and UI actions |

## OpenAI Connection

```text
Base URL: http://<phone-ip>:11434/v1
Model: one id from GET /v1/models
API Key: blank or any non-empty string, depending on the client
```

Recommended request shape:

```json
{
  "stream": true,
  "max_tokens": 256
}
```

Use shorter contexts and smaller tool schemas for phone-side models.

## Local Tools

Current Lociant-local tools:

| Tool | Purpose |
|---|---|
 | `runtime_status` | API/runtime status |
 | `device_status` | Battery, network, screen, and permission state |
 | `clipboard_read` | Read Android clipboard text when available |
 | `clipboard_write` | Write Android clipboard text |
 | `app_open` | Open an installed app or safe deep link |
 | `ui_screen_state` | One-call screen context: device state, permissions, foreground package, UI text, actionable nodes, and optional screenshot |
 | `model_list` | Installed/built-in models |
 | `llm_status` | Phone-local LLM readiness and available chat models |
 | `llm_chat` | Ask the phone-local LLM through MCP or direct tool calls |
 | `vision_status` | Camera/vision runtime status |
 | `vision_start` | Start continuous camera vision analysis |
 | `camera_capture` | Capture the latest camera frame as a JPEG data URL |
 | `vision_stop` | Stop continuous camera vision analysis |
 | `ui_click_node` | Click a `nodeId` returned by `ui_screen_state` |
 | `ui_tap` | Tap at screen coordinates |
 | `ui_swipe` | Swipe between two screen coordinates |
 | `ui_back` | Press the Android Back button |
 | `ui_home` | Press the Android Home button |
 | `ui_recent_apps` | Open the Android recent apps overview |
 | `ui_notifications` | Open the Android notification shade |
 | `ui_quick_settings` | Open Android quick settings |
 | `ui_wait` | Wait for a fixed duration, UI idle, or visible text after an action |

These tools should describe Android-side capabilities. Do not add PC workspace tools to Lociant unless they map to real phone-side behavior.

For desktop agents, `llm_chat` is the simplest way to use the Android phone as a local reasoning node through MCP:

```json
{
  "prompt": "Summarize why a phone-local model is useful for an agent.",
  "maxTokens": 128
}
```

`llm_chat` also accepts `image` or `images` as data URLs, and OpenAI-style `messages[].content[].image_url`. Image input requires a VLM model. By default `llm_chat` does not persist chat history. Pass `sessionId` when you want Lociant to reuse and save phone-local context.

To analyze the current Android screen without manually passing a screenshot, call `llm_chat` with `useScreenFrame: true`:

```json
{
  "prompt": "Describe the current phone screen and suggest the next tap.",
  "useScreenFrame": true,
  "maxTokens": 160
}
```

When the image comes from the phone camera, prefer `useCameraFrame: true` after starting vision. MCP responses intentionally compact large media in `structuredContent`, so copying the placeholder text from `camera_capture` back into `llm_chat` will not work.

 For external agents such as Hermes, prefer `ui_screen_state` when they need phone state in one call:

```json
{
  "includeScreenshot": true,
  "maxNodes": 40,
  "screenshotMaxWidth": 720
}
```

 `ui_screen_state` is intentionally read-only. It returns current device/network/permission state plus a compact accessibility summary with `nodeId` values. If the agent needs to act, it should explicitly call `ui_click_node`, `ui_tap`, `ui_swipe`, `ui_back`, `ui_home`, `ui_recent_apps`, `ui_notifications`, `ui_quick_settings`, or another action tool after inspecting the context.


For timing, use `ui_wait` with `durationMs` when the agent needs a real fixed sleep:

```json
{
  "durationMs": 1000
}
```

Use `text` or `idleMs` when the agent is waiting for a screen condition instead. For compatibility, `ui_wait` also treats `timeoutMs` as a fixed duration when no `text` or `idleMs` is provided.

## Hermes On Android

Hermes is better treated as an agent runtime that connects to Lociant, not code that should be embedded directly into the APK. The practical Android setup is:

```text
Termux on Android
  -> runs Hermes / agent CLI
  -> connects to Lociant MCP at http://127.0.0.1:11434/mcp or LAN IP

Lociant app
  -> owns Android permissions, foreground service, local LLM/VLM, camera, screen tools
  -> exposes OpenAI-compatible API and MCP tools
```

 This keeps Android permissions and native runtime inside the app, while Hermes keeps ownership of planning and agent state. Start Lociant Runtime first, then add the phone MCP endpoint to Hermes. Use `ui_screen_state` for observation, `llm_chat` for phone-local reasoning, and the `ui_*` tools only when a user-approved action is needed.

For photo capture, use `camera_capture`. It intentionally reuses the active vision runtime instead of opening a second camera path. Start vision first, then capture:

```bash
python scripts/lociant_capture.py --base-url http://<phone-ip>:11434 --start --out capture.jpg
```

## MCP

For MCP-native clients that support Streamable HTTP, connect directly to the phone:

```text
Name: Lociant
Transport: Streamable HTTP
URL: http://<phone-ip>:11434/mcp
Headers: empty
```

If API Token is enabled, add the same `Authorization: Bearer <token>` header here too.

Clients that only support command-based stdio can use the desktop adapter:

```bash
python scripts/lociant_mcp_server.py --base-url http://<phone-ip>:11434
```

If API Token is enabled, add `--api-key <token>`.

Example client config:

```json
{
  "mcpServers": {
    "lociant": {
      "command": "python",
      "args": [
        "C:/Users/Lhx/Documents/Programs/Lociant/scripts/lociant_mcp_server.py",
        "--base-url",
        "http://<phone-ip>:11434"
      ]
    }
  }
}
```

With API Token enabled:

```json
{
  "mcpServers": {
    "lociant": {
      "command": "python",
      "args": [
        "C:/Users/Lhx/Documents/Programs/Lociant/scripts/lociant_mcp_server.py",
        "--base-url",
        "http://<phone-ip>:11434",
        "--api-key",
        "<token>"
      ]
    }
  }
}
```

Both paths expose the same underlying `ToolRegistry`. MCP is only a protocol adapter over `/v1/tools`, not another capability system. Prefer the phone-native `/mcp` endpoint when the client supports Streamable HTTP.


## Client-Owned Tools

An agent client may send its own OpenAI `tools`, for example `read`, `edit`, or `bash`. Lociant should pass those schemas into the model and return standard `tool_calls` when the model selects one.

The client should execute those client-owned tools. Lociant should only execute tools that exist in its local registry and only when the request explicitly enables local execution.

## Debugging

Quick runtime/API/MCP test:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434
```

Include one chat completion:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --chat
```

Test the MCP path into the phone-local LLM:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --mcp-llm
```

With an image:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --mcp-llm --mcp-llm-image ./capture.jpg --prompt "What is in this image?"
```

With the current phone camera frame:

```json
{
  "prompt": "What does the phone camera see?",
  "useCameraFrame": true,
  "maxTokens": 128
}
```

If API Token is enabled:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --api-key <token> --expect-auth --chat
```

Full protocol test:

```bash
python scripts/lociant_test.py full --base-url http://<phone-ip>:11434
```

Logging proxy:

```bash
python scripts/lociant_test.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
```

Point the agent client to:

```text
http://127.0.0.1:11435/v1
```

The proxy helps answer:

- Did the client send `tools`?
- Did it use `tool_choice`?
- Did it stream?
- Did it send `role: "tool"` follow-up messages?
- Did Lociant return standard `tool_calls`?
- How long did each request take?

## Performance Expectations

Local phone models are useful, but they are not desktop-class agent brains. For coding agents, Lociant is best used as:

- a local model endpoint for light tasks
- a phone camera / sensor / notification tool provider
- an always-nearby LAN capability node

Heavy repository reasoning, large file ingestion, and long multi-step plans should still be sized carefully or delegated to a stronger model.

---

## 中文

Lociant 是 agent 系统中的手机侧能力 provider。它不是 agent harness。

## 职责划分

```text
Agent client
  -> planning
  -> workspace tools
  -> shell / edit / read / grep
  -> task state and UI
  -> OpenAI-compatible calls to Lociant

Android 上的 Lociant
  -> local LLM / VLM
  -> camera / vision
  -> sensors and runtime state
  -> Android screen context
  -> explicit Android UI actions
```

这个划分可以让手机持续有用，而不要求它伪装成桌面代码工作站。

## 快速开始

1. 在 Android 手机上安装 Lociant，并启动 Runtime。
2. 确认手机显示局域网地址，例如：

```text
http://10.238.125.4:11434
```

3. 在 agent 客户端添加 MCP server：

```text
名称：Lociant
传输类型：Streamable HTTP
服务器地址：http://<phone-ip>:11434/mcp
请求头：留空
```

如果在 Runtime 设置里启用了 API Token，请添加：

```text
Authorization: Bearer <token>
```

4. 在当前对话里启用 MCP server / tools。
5. 用下面的话测试：

```text
调用 runtime_status。
启动视觉，然后调用 camera_capture。
```

添加 MCP server 只是安装工具源。大多数客户端还需要在每个对话或助手配置里启用这些工具。

Runtime 设置可以限制暴露的工具。在 App 内 WebView UI 中操作：设置 → 能力 → 远程工具。

| 级别 | 暴露能力 |
|---|---|
| `read` | runtime/model 状态 |
| `sensor` | 只读工具，以及摄像头/视觉工具 |
| `action` | 全部本地工具，包括通知和 webhook |

## OpenAI 连接

```text
Base URL: http://<phone-ip>:11434/v1
Model: 从 GET /v1/models 返回值里选择
API Key: 留空或任意非空字符串，取决于客户端
```

推荐请求形态：

```json
{
  "stream": true,
  "max_tokens": 256
}
```

手机侧模型建议使用更短上下文和更小的工具 schema。

## 本地工具

当前 Lociant 本地工具：

| Tool | 用途 |
|---|---|
 | `runtime_status` | API/runtime 状态 |
 | `device_status` | 电量、网络、屏幕和权限状态 |
 | `clipboard_read` | 在系统允许时读取 Android 剪贴板文本 |
 | `clipboard_write` | 写入 Android 剪贴板文本 |
 | `app_open` | 打开已安装应用或安全 deep link |
 | `ui_screen_state` | 一次性读取屏幕上下文、设备状态、可操作 `nodeId` 和可选截图 |
 | `model_list` | 已安装和内置模型 |
 | `llm_status` | 手机本地 LLM 就绪状态和可用对话模型 |
 | `llm_chat` | 通过 MCP 或直接调用向手机本地 LLM 提问 |
 | `vision_status` | 摄像头/视觉 runtime 状态 |
 | `vision_start` | 启动连续摄像头视觉分析 |
 | `camera_capture` | 将最新摄像头画面捕获为 JPEG data URL |
 | `vision_stop` | 停止连续摄像头视觉分析 |
 | `ui_click_node` | 点击 `ui_screen_state` 返回的 `nodeId` |
 | `ui_tap` | 在屏幕坐标处点击 |
 | `ui_swipe` | 在两个屏幕坐标之间滑动 |
 | `ui_back` | 按 Android 返回键 |
 | `ui_home` | 按 Android Home 键 |
 | `ui_recent_apps` | 打开 Android 最近任务视图 |
 | `ui_notifications` | 打开 Android 通知栏 |
 | `ui_quick_settings` | 打开 Android 快捷设置 |
 | `ui_wait` | 动作后等待 UI 空闲或等待文字出现 |

这些工具应该描述 Android 侧能力。不要把 PC 工作区工具加进 Lociant，除非它真的映射到手机侧行为。

获取手机照片时使用 `camera_capture`。它会复用当前 vision runtime，不打开第二套摄像头链路。先启动 vision，再捕获：

```bash
python scripts/lociant_capture.py --base-url http://<phone-ip>:11434 --start --out capture.jpg
```

## MCP

支持 Streamable HTTP 的 MCP-native 客户端可以直接连手机：

```text
名称：Lociant
传输类型：Streamable HTTP
服务器地址：http://<phone-ip>:11434/mcp
请求头：留空
```

如果启用了 API Token，这里也要添加 `Authorization: Bearer <token>`。

只支持命令式 stdio 的客户端可以使用桌面端 adapter：

```bash
python scripts/lociant_mcp_server.py --base-url http://<phone-ip>:11434
```

如果启用了 API Token，加上 `--api-key <token>`。

客户端配置示例：

```json
{
  "mcpServers": {
    "lociant": {
      "command": "python",
      "args": [
        "C:/Users/Lhx/Documents/Programs/Lociant/scripts/lociant_mcp_server.py",
        "--base-url",
        "http://<phone-ip>:11434"
      ]
    }
  }
}
```

启用 API Token 时：

```json
{
  "mcpServers": {
    "lociant": {
      "command": "python",
      "args": [
        "C:/Users/Lhx/Documents/Programs/Lociant/scripts/lociant_mcp_server.py",
        "--base-url",
        "http://<phone-ip>:11434",
        "--api-key",
        "<token>"
      ]
    }
  }
}
```

两条路径都暴露同一个底层 `ToolRegistry`。MCP 只是 `/v1/tools` 之上的协议适配层，不是第二套能力系统。客户端支持 Streamable HTTP 时，优先使用手机端原生 `/mcp`。

## 客户端自有工具

Agent 客户端可以传入自己的 OpenAI `tools`，例如 `read`、`edit` 或 `bash`。Lociant 应该把这些 schema 传给模型，并在模型选择工具时返回标准 `tool_calls`。

这些客户端自有工具应该由客户端执行。Lociant 只应该执行存在于本地 registry 的工具，并且只有在请求显式启用本地执行时才执行。

## 调试

快速 Runtime/API/MCP 测试：

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434
```

包含一次 chat completion：

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --chat
```

如果启用了 API Token：

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --api-key <token> --expect-auth --chat
```

完整协议测试：

```bash
python scripts/lociant_test.py full --base-url http://<phone-ip>:11434
```

日志代理：

```bash
python scripts/lociant_test.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
```

把 agent 客户端指向：

```text
http://127.0.0.1:11435/v1
```

代理可以帮助确认：

- 客户端是否发送了 `tools`
- 是否使用了 `tool_choice`
- 是否使用流式
- 是否发送了 `role: "tool"` follow-up messages
- Lociant 是否返回标准 `tool_calls`
- 每个请求耗时多久

## 性能预期

手机本地模型是有用的，但它不是桌面级 agent 大脑。对 coding agent 来说，Lociant 更适合：

- 作为轻任务的本地模型端点
- 作为手机摄像头/传感器/通知工具 provider
- 作为随手可用的局域网能力节点

重型仓库推理、大文件输入和长链路多步规划仍然需要控制规模，或者交给更强的模型。
