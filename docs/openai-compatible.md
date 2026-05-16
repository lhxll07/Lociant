# OpenAI-Compatible API

> Version: 0.2 | Updated: 2026-05-17

[English](#english) · [中文](#中文)

## English

MNNode can be used as a local OpenAI-compatible upstream provider for agent clients such as RikkaHub, OpenClaw-style runtimes, LangChain, or custom scripts.

The goal is not to replace agent frameworks. MNNode is the Android provider node that supplies local models, vision, runtime state, and device tools through standard HTTP APIs.

## Provider Role

MNNode is not trying to be another chat app. It is closer to an upstream API supply layer:

```text
Agent / Automation Client
  -> OpenAI-compatible chat and tools
  -> MNNode
      -> local MNN model
      -> local vision tools
      -> runtime and device state
      -> persisted sessions and events
```

This is the key difference from a model-only server. A client can use MNNode as a local replacement for cloud model calls today, and later as a gateway to Android device capabilities through OpenAI-style tools.

## Connection

Use the phone's LAN address:

```text
Base URL: http://<phone-ip>:11434/v1
API Key: any non-empty string, or blank if the client allows it
Model: one id from GET /v1/models
```

MNNode also accepts root-style URLs internally:

```text
http://<phone-ip>:11434/v1/chat/completions
http://<phone-ip>:11434/v1/models
http://<phone-ip>:11434/v1/tools
```

OpenClaw-style configuration:

```yaml
llm:
  provider: openai
  api_base: http://<phone-ip>:11434/v1
  api_key: ""
  model: qwen3.5-2b-mnn
  stream: true
```

## Supported Surface

- `GET /health`
- `GET /v1/models`
- `GET /v1/tools`
- `POST /v1/tools/{name}/call`
- `POST /v1/chat/completions`
- `POST /api/chat`
- text chat
- image chat
- OpenAI SSE text streaming
- Ollama NDJSON text streaming
- serialized multi-request queue
- explicit session persistence
- OpenAI-style `tools`
- OpenAI-style `tool_choice`
- OpenAI-style `tool_calls`
- OpenAI-style `role: "tool"` follow-up messages
- OpenAI-style token usage, including `cached_tokens`
- MNNode runtime metrics: first token, prefill, decode speed, wall speed

Defaults:

```text
host: 0.0.0.0
port: 11434
start: manual
concurrency: one native model request at a time
background: foreground runtime service + optional Runtime Window
```

The model server is runtime infrastructure controlled from Settings -> Runtime. It is not a Scene Pack.

## Sessions

Explicit sessions are persisted in Room. Supply a session id through:

- `X-MNNode-Session-Id`
- `X-Session-Id`
- `sessionId`
- `session_id`
- `metadata.sessionId`
- `metadata.session_id`

Anonymous API requests are served but are not durable chat sessions by default.

## Token and Cache Policy

Output length is unified as:

```text
min(request value or runtime default 512, runtime hard cap 32768)
```

Supported request fields:

- OpenAI: `max_tokens`
- Ollama: `options.num_predict`
- scene/client calls: `maxTokens`

Model `config.json` values such as `max_new_tokens` are reported for diagnostics but do not override explicit API/runtime settings.

The current prompt cache is conservative:

- process-local
- text-only
- optimized for linear chat histories
- reset on image input, restart, model/config change, or divergent history

Room remains the durable conversation source. Cache is only a runtime optimization.

## Performance and Usage Metrics

MNNode returns standard OpenAI `usage` fields:

```json
{
  "usage": {
    "prompt_tokens": 170,
    "completion_tokens": 64,
    "total_tokens": 234,
    "prompt_tokens_details": {
      "cached_tokens": 83
    }
  }
}
```

It also adds an `mnnode` diagnostics object:

```json
{
  "mnnode": {
    "first_token_ms": 650,
    "prefill_us": 530000,
    "decode_us": 2238000,
    "decode_tokens_per_second": 28.6,
    "wall_tokens_per_second": 22.1,
    "cache": {
      "enabled": true,
      "hit": true
    }
  }
}
```

Use `decode_tokens_per_second` to compare raw generation speed. Use `first_token_ms`, `prefill_us`, and `cached_tokens` to diagnose perceived latency.

Current `Qwen3.5-2B-MNN` measurements on the test phone are roughly:

| Case | First Token | Decode |
|---|---:|---:|
| short prompt | ~0.2s | ~28-30 tok/s |
| first long-context turn | ~1.7-3.0s | ~28-30 tok/s |
| linear long chat with prompt cache | ~0.6-1.1s after first turn | ~28-30 tok/s |

This is why MNNode feels close to MNN Chat in decode speed. If a client edits, retries, or sends divergent histories, the single process-local prompt cache may miss and first-token latency can rise again.

## Tool Calling

MNNode exposes one shared tool registry. Direct tool calls and OpenAI chat tool calls use the same definitions and handlers. This turns Android capabilities into API-callable tools instead of private app features.

Tool definitions include MNNode policy metadata:

```json
{
  "x_execution": "local",
  "x_policy": {
    "local": true,
    "remoteAllowed": true,
    "requiresActivity": true,
    "sideEffect": true
  }
}
```

- `requiresActivity`: the interactive Activity must be alive, usually for CameraX / WebView permission flows.
- `sideEffect`: the tool changes runtime state, storage, notifications, or external systems.
- `remoteAllowed`: the tool may be shown to LAN API clients. Future auth policy can use this field directly.

Default behavior follows the OpenAI pattern: a forced `tool_choice` returns a response with `finish_reason: "tool_calls"`. The upstream client can execute the tool and send the result back as a `role: "tool"` message.

For MNNode-local execution, set:

```json
{
  "execute_tools": true
}
```

Then MNNode executes the selected local tool, appends the tool result to the model context, and returns a final assistant message.

`tool_choice: "auto"` is intentionally conservative for now. MNNode does not yet rely on small local models to autonomously choose tools.

Near-term tool direction:

- expose model/runtime status
- expose local vision status and rules
- expose object detection results
- expose event history
- later add embeddings and local retrieval

This makes MNNode a practical upstream provider for OpenClaw-style agents: the agent plans, MNNode supplies local model and device capabilities.

## Compatibility Test

Run the smoke test from the repository root:

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1
```

Endpoint-only check:

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1 --skip-chat
```

Skip tool checks:

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1 --skip-tools
```

On Windows, this Python script is preferred over PowerShell web cmdlets because it avoids shell-specific JSON and streaming behavior.

For low-level debugging, prefer `curl.exe --http1.1` over PowerShell web cmdlets when testing POST or streaming behavior.

## Known Limits

- Streaming `delta.tool_calls` is not implemented yet.
- `tool_choice: "auto"` does not yet perform model-driven tool selection.
- Only one native model inference runs at a time.
- LAN auth is still future work, so do not expose the server to the public internet.
- URLs inside image messages are not fetched; use inline base64 data URLs.

---

## 中文

MNNode 可以作为本地 OpenAI 兼容上游 Provider，被 RikkaHub、OpenClaw 类 runtime、LangChain 或自定义脚本直接调用。

目标不是替代 agent 框架。MNNode 的定位是 Android provider node，通过标准 HTTP API 提供本地模型、视觉、runtime 状态和设备工具。

## Provider 定位

MNNode 不想成为另一个聊天 App。它更接近上游 API 供应层：

```text
Agent / Automation Client
  -> OpenAI-compatible chat and tools
  -> MNNode
      -> local MNN model
      -> local vision tools
      -> runtime and device state
      -> persisted sessions and events
```

这是它和纯模型服务器的关键区别。客户端现在可以把 MNNode 当成本地云 API 替代品，以后还可以通过 OpenAI 风格 tools 调用 Android 设备能力。

## 连接方式

使用手机的局域网地址：

```text
Base URL: http://<phone-ip>:11434/v1
API Key: 任意非空字符串；如果客户端允许，也可以留空
Model: 从 GET /v1/models 返回值中选择
```

MNNode 内部实际端点：

```text
http://<phone-ip>:11434/v1/chat/completions
http://<phone-ip>:11434/v1/models
http://<phone-ip>:11434/v1/tools
```

OpenClaw 类配置：

```yaml
llm:
  provider: openai
  api_base: http://<phone-ip>:11434/v1
  api_key: ""
  model: qwen3.5-2b-mnn
  stream: true
```

## 已支持能力

- `GET /health`
- `GET /v1/models`
- `GET /v1/tools`
- `POST /v1/tools/{name}/call`
- `POST /v1/chat/completions`
- `POST /api/chat`
- 文本对话
- 图片对话
- OpenAI SSE 文本流式输出
- Ollama NDJSON 文本流式输出
- 多请求串行队列
- 显式 session 持久化
- OpenAI 风格 `tools`
- OpenAI 风格 `tool_choice`
- OpenAI 风格 `tool_calls`
- OpenAI 风格 `role: "tool"` 后续消息
- OpenAI 风格 token usage，包括 `cached_tokens`
- MNNode runtime 指标：首 token、prefill、decode 速度、wall speed

默认配置：

```text
host: 0.0.0.0
port: 11434
启动: 手动
并发: 同一时间一个 native 模型请求
后台: 前台 runtime service + 可选 Runtime Window
```

模型服务是由 Settings -> Runtime 控制的 runtime 基础设施，不是 Scene Pack。

## Sessions

显式 session 会持久化到 Room。可通过以下方式传入 session id：

- `X-MNNode-Session-Id`
- `X-Session-Id`
- `sessionId`
- `session_id`
- `metadata.sessionId`
- `metadata.session_id`

匿名 API 请求会正常处理，但默认不作为持久对话 session。

## Token 和 Cache 策略

输出长度统一为：

```text
min(请求值或 runtime 默认 512, runtime 硬上限 32768)
```

支持字段：

- OpenAI: `max_tokens`
- Ollama: `options.num_predict`
- scene/client calls: `maxTokens`

模型 `config.json` 里的 `max_new_tokens` 会作为诊断信息展示，但不会覆盖明确的 API/runtime 设置。

当前 prompt cache 保持保守：

- 仅进程内
- 仅文本
- 优先优化线性聊天 history
- 图片输入、重启、模型/配置变化或分叉 history 时可能重置

Room 仍然是持久对话事实源。Cache 只是 runtime 优化。

## 性能与 Usage 指标

MNNode 返回标准 OpenAI `usage` 字段：

```json
{
  "usage": {
    "prompt_tokens": 170,
    "completion_tokens": 64,
    "total_tokens": 234,
    "prompt_tokens_details": {
      "cached_tokens": 83
    }
  }
}
```

同时会附加 `mnnode` 诊断对象：

```json
{
  "mnnode": {
    "first_token_ms": 650,
    "prefill_us": 530000,
    "decode_us": 2238000,
    "decode_tokens_per_second": 28.6,
    "wall_tokens_per_second": 22.1,
    "cache": {
      "enabled": true,
      "hit": true
    }
  }
}
```

对比原始生成速度看 `decode_tokens_per_second`。诊断体感延迟看 `first_token_ms`、`prefill_us` 和 `cached_tokens`。

当前测试手机上，`Qwen3.5-2B-MNN` 大致结果：

| 场景 | 首 token | Decode |
|---|---:|---:|
| 短 prompt | 约 0.2s | 约 28-30 tok/s |
| 长上下文首轮 | 约 1.7-3.0s | 约 28-30 tok/s |
| 线性长对话且 prompt cache 命中 | 首轮后约 0.6-1.1s | 约 28-30 tok/s |

这也是 MNNode 在 decode 速度上接近 MNN Chat 的原因。如果客户端编辑、重试或发送分叉 history，单进程 prompt cache 可能 miss，首 token 延迟会重新升高。

## 工具调用

MNNode 只有一套共享工具注册表。直接工具调用和 OpenAI chat 工具调用复用同一份定义和处理逻辑。这样 Android 能力会变成 API 可调用工具，而不是私有 App 功能。

工具定义带有 MNNode 策略元数据：

```json
{
  "x_execution": "local",
  "x_policy": {
    "local": true,
    "remoteAllowed": true,
    "requiresActivity": true,
    "sideEffect": true
  }
}
```

- `requiresActivity`：需要交互式 Activity 存活，通常用于 CameraX / WebView 权限链路。
- `sideEffect`：工具会改变 runtime 状态、存储、通知或外部系统。
- `remoteAllowed`：工具可以展示给局域网 API 客户端。后续认证策略可以直接使用这个字段。

默认行为贴近 OpenAI：强制 `tool_choice` 会返回 `finish_reason: "tool_calls"`，上游客户端执行工具后，再把结果作为 `role: "tool"` 消息发回来。

如果要让 MNNode 在本地执行工具，传入：

```json
{
  "execute_tools": true
}
```

这样 MNNode 会执行被选中的本地工具，把工具结果加入模型上下文，然后返回最终 assistant 消息。

`tool_choice: "auto"` 目前刻意保持保守。MNNode 还不会依赖小型本地模型自动选择工具。

近期工具方向：

- 暴露模型 / runtime 状态
- 暴露本地视觉状态和规则
- 暴露目标检测结果
- 暴露事件历史
- 后续增加 embeddings 和本地检索

这会让 MNNode 成为 OpenClaw 类 agent 的实用上游 Provider：agent 负责规划，MNNode 提供本地模型和设备能力。

## 兼容性测试

在仓库根目录运行：

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1
```

只检查基础端点：

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1 --skip-chat
```

跳过工具调用检查：

```bash
python scripts/model_server_smoke.py --base-url http://<phone-ip>:11434/v1 --skip-tools
```

在 Windows 上，优先使用这个 Python 脚本，而不是 PowerShell web cmdlets，因为它能避开 JSON 引号和流式请求行为差异。

底层调试 POST 或 streaming 行为时，优先用 `curl.exe --http1.1`，少用 PowerShell web cmdlets。

## 当前限制

- 尚未实现流式 `delta.tool_calls`。
- `tool_choice: "auto"` 尚未做模型自动工具选择。
- 同一时间只运行一个 native 模型推理。
- 局域网认证仍是后续工作，不要把服务暴露到公网。
- 图片消息里的 URL 不会被拉取；请使用内联 base64 data URL。
