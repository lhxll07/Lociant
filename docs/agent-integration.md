# Agent Integration

> Version: 0.2 | Updated: 2026-05-18

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
  -> notifications
  -> sessions, events, and local storage
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

4. Enable the MCP server/tools in the current chat.
5. Test with:

```text
Call runtime_status.
Start vision, then call camera_capture.
```

Adding the MCP server only installs the tool source. Most clients still require enabling those tools in each chat or assistant profile.

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
| `runtime_resources` | Android package and resource info |
| `model_list` | Installed/built-in models |
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

These tools should describe Android-side capabilities. Do not add PC workspace tools to Lociant unless they map to real phone-side behavior.

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

Clients that only support command-based stdio can use the desktop adapter:

```bash
python scripts/lociant_mcp_server.py --base-url http://<phone-ip>:11434
```

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

Both paths expose the same underlying `ToolRegistry`. MCP is only a protocol adapter over `/v1/tools`, not another capability system. Prefer the phone-native `/mcp` endpoint when the client supports Streamable HTTP.

## Client-Owned Tools

An agent client may send its own OpenAI `tools`, for example `read`, `edit`, or `bash`. Lociant should pass those schemas into the model and return standard `tool_calls` when the model selects one.

The client should execute those client-owned tools. Lociant should only execute tools that exist in its local registry and only when the request explicitly enables local execution.

## Debugging

Smoke test:

```bash
python scripts/openai_agent_probe.py smoke --base-url http://<phone-ip>:11434/v1
```

Logging proxy:

```bash
python scripts/openai_agent_probe.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
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
  -> notifications
  -> sessions, events, and local storage
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

4. 在当前对话里启用 MCP server / tools。
5. 用下面的话测试：

```text
调用 runtime_status。
启动视觉，然后调用 camera_capture。
```

添加 MCP server 只是安装工具源。大多数客户端还需要在每个对话或助手配置里启用这些工具。

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

只支持命令式 stdio 的客户端可以使用桌面端 adapter：

```bash
python scripts/lociant_mcp_server.py --base-url http://<phone-ip>:11434
```

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

两条路径都暴露同一个底层 `ToolRegistry`。MCP 只是 `/v1/tools` 之上的协议适配层，不是第二套能力系统。客户端支持 Streamable HTTP 时，优先使用手机端原生 `/mcp`。

## 客户端自有工具

Agent 客户端可以传入自己的 OpenAI `tools`，例如 `read`、`edit` 或 `bash`。Lociant 应该把这些 schema 传给模型，并在模型选择工具时返回标准 `tool_calls`。

这些客户端自有工具应该由客户端执行。Lociant 只应该执行存在于本地 registry 的工具，并且只有在请求显式启用本地执行时才执行。

## 调试

基础测试：

```bash
python scripts/openai_agent_probe.py smoke --base-url http://<phone-ip>:11434/v1
```

日志代理：

```bash
python scripts/openai_agent_probe.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
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
