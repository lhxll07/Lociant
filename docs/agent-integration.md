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

## Connection Profile

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
| `vision_stop` | Stop continuous camera vision analysis |
| `event_record` | Persist a runtime event |
| `store_increment` | Increment a numeric local-store value |
| `notification_post` | Send Android notification |
| `webhook_post` | Queue JSON webhook POST |

These tools should describe Android-side capabilities. Do not add PC workspace tools to Lociant unless they map to real phone-side behavior.

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

## 连接配置

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
| `vision_stop` | 停止连续摄像头视觉分析 |
| `event_record` | 持久化 runtime 事件 |
| `store_increment` | 递增本地存储里的数值 |
| `notification_post` | 发送 Android 通知 |
| `webhook_post` | 排队发送 JSON webhook |

这些工具应该描述 Android 侧能力。不要把 PC 工作区工具加进 Lociant，除非它真的映射到手机侧行为。

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
