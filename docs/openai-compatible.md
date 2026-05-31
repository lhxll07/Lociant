# OpenAI-Compatible API

> Version: 0.3 | Updated: 2026-05-18

[English](#english) | [中文](#中文)

## English

Lociant can be used as a local OpenAI-compatible upstream provider for agent clients, chat clients, and LAN automation.

It is not a replacement for Pi, OpenClaw, OpenCode, RikkaHub, LangChain, or custom agent frameworks. Those systems should own the agent loop and workspace tools. Lociant supplies local model calls and Android phone capabilities.

## Connection

```text
Base URL: http://<phone-ip>:11434/v1
Model: one id from GET /v1/models
API Key: blank or any non-empty string, depending on the client
```

Common endpoints:

```text
GET  /health
GET  /v1/models
POST /v1/chat/completions
POST /api/chat
GET  /v1/tools
POST /v1/tools/{name}/call
```

Keep the server on trusted LAN only.

Runtime settings can enable an API Token. When enabled, LAN clients must send:

```text
Authorization: Bearer <token>
```

This applies to chat, tools, and MCP. `/health` stays public for simple discovery and diagnostics.

## Chat

Minimal OpenAI-style request:

```json
{
  "model": "qwen3.5-2b-mnn",
  "messages": [
    { "role": "user", "content": "Hello" }
  ],
  "stream": true,
  "max_tokens": 256
}
```

Lociant accepts unknown OpenAI fields when they can be safely ignored. This is important for broad client compatibility.

## Usage And Metrics

Responses include standard OpenAI `usage` fields:

```json
{
  "usage": {
    "prompt_tokens": 42,
    "completion_tokens": 128,
    "total_tokens": 170,
    "prompt_tokens_details": {
      "cached_tokens": 20
    }
  }
}
```

Responses also include a compatibility diagnostics object currently named `mnnode`:

```json
{
  "mnnode": {
    "first_token_ms": 530,
    "prefill_tokens_per_second": 90.1,
    "decode_tokens_per_second": 29.4,
    "wall_tokens_per_second": 24.8,
    "elapsed_ms": 5160
  }
}
```

The field name remains `mnnode` for compatibility with existing scripts and clients. It can be aliased later if needed.

## Sessions

The API supports explicit session persistence. Existing clients may use:

```text
X-MNNode-Session-Id: <session-id>
```

If the header is absent, Lociant can still answer stateless requests. Session behavior is most useful for clients that want durable phone-side chat history.

## Tools

Lociant exposes one shared local tool registry. Direct HTTP tool calls and OpenAI chat tool calls use the same definitions and handlers.

List tools:

```bash
curl http://<phone-ip>:11434/v1/tools
```

Call a local tool directly:

```bash
curl -X POST http://<phone-ip>:11434/v1/tools/runtime_status/call \
  -H "Content-Type: application/json" \
  -d "{\"arguments\":{}}"
```

Use the phone-local LLM as a tool, including through MCP:

```bash
curl -X POST http://<phone-ip>:11434/v1/tools/llm_chat/call \
  -H "Content-Type: application/json" \
  -d "{\"arguments\":{\"prompt\":\"Give one practical use for a phone-local AI node.\",\"maxTokens\":128}}"
```

For VLM models, pass `image` or `images` as data URLs:

```json
{
  "arguments": {
    "prompt": "What is in this image?",
    "image": "data:image/jpeg;base64,...",
    "maxTokens": 128
  }
}
```

For the phone camera path, start vision first and then call:

```json
{
  "arguments": {
    "prompt": "What does the phone camera see?",
    "useCameraFrame": true,
    "maxTokens": 128
  }
}
```

Do not copy the compact placeholder from MCP `camera_capture` text output into `llm_chat`; large media is intentionally compacted there.

Use OpenAI-style tool calling:

```json
{
  "model": "qwen3.5-2b-mnn",
  "messages": [
    { "role": "user", "content": "Check the phone runtime status." }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "runtime_status",
        "description": "Return API/runtime status.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    }
  ],
  "tool_choice": {
    "type": "function",
    "function": { "name": "runtime_status" }
  }
}
```

`tool_choice: "auto"` is model/template-driven. If a model package lacks a tools-aware chat template, Lociant will not invent reliable tool arguments.

Set `execute_tools: true` only when you want Lociant to execute a selected local tool and feed the result back to the model. Client-owned tools such as `read`, `edit`, `bash`, and `grep` should be executed by the client.

## Agent Probe

Smoke test:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --chat
```

MCP tool path into the phone-local LLM:

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --mcp-llm
```

Logging proxy:

```bash
python scripts/lociant_test.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
```

Point the agent to:

```text
http://127.0.0.1:11435/v1
```

The proxy shows request shape, tool schemas, streaming behavior, latency, and whether Lociant returned `tool_calls`.

## Practical Defaults

For small local phone models:

- use `stream: true`
- keep `max_tokens` modest, often `128` to `512`
- keep tool schemas short
- avoid sending huge workspace files through the phone model
- let desktop agents execute desktop tools
- use Lociant for local model calls and phone-side sensing/actions

---

## 中文

Lociant 可以作为本地 OpenAI-compatible 上游 provider，被 agent 客户端、聊天客户端和局域网自动化调用。

它不是 Pi、OpenClaw、OpenCode、RikkaHub、LangChain 或自定义 agent framework 的替代品。这些系统应该负责 agent loop 和工作区工具。Lociant 提供本地模型调用和 Android 手机能力。

## 连接

```text
Base URL: http://<phone-ip>:11434/v1
Model: 从 GET /v1/models 返回值里选择
API Key: 留空或任意非空字符串，取决于客户端
```

常用端点：

```text
GET  /health
GET  /v1/models
POST /v1/chat/completions
POST /api/chat
GET  /v1/tools
POST /v1/tools/{name}/call
```

只建议在可信局域网使用。

Runtime 设置可以启用 API Token。启用后，局域网客户端必须发送：

```text
Authorization: Bearer <token>
```

这个 token 适用于 chat、tools 和 MCP。`/health` 保持公开，便于发现和诊断。

## Chat

最小 OpenAI 风格请求：

```json
{
  "model": "qwen3.5-2b-mnn",
  "messages": [
    { "role": "user", "content": "Hello" }
  ],
  "stream": true,
  "max_tokens": 256
}
```

Lociant 会在安全可忽略的情况下接受未知 OpenAI 字段。这对广泛客户端兼容很重要。

## Usage 和指标

响应包含标准 OpenAI `usage` 字段：

```json
{
  "usage": {
    "prompt_tokens": 42,
    "completion_tokens": 128,
    "total_tokens": 170,
    "prompt_tokens_details": {
      "cached_tokens": 20
    }
  }
}
```

响应还包含一个当前名为 `mnnode` 的兼容诊断对象：

```json
{
  "mnnode": {
    "first_token_ms": 530,
    "prefill_tokens_per_second": 90.1,
    "decode_tokens_per_second": 29.4,
    "wall_tokens_per_second": 24.8,
    "elapsed_ms": 5160
  }
}
```

字段名暂时保留为 `mnnode`，用于兼容现有脚本和客户端。后续需要时可以增加别名。

## Sessions

API 支持显式 session 持久化。现有客户端可以使用：

```text
X-MNNode-Session-Id: <session-id>
```

如果没有这个 header，Lociant 仍然可以回答无状态请求。Session 更适合需要手机侧持久对话历史的客户端。

## Tools

Lociant 只有一套共享本地工具注册表。直接 HTTP tool call 和 OpenAI chat tool call 复用同一份定义和处理逻辑。

列出工具：

```bash
curl http://<phone-ip>:11434/v1/tools
```

直接调用本地工具：

```bash
curl -X POST http://<phone-ip>:11434/v1/tools/runtime_status/call \
  -H "Content-Type: application/json" \
  -d "{\"arguments\":{}}"
```

把手机本地 LLM 当作工具调用，MCP 也会暴露同一个能力：

```bash
curl -X POST http://<phone-ip>:11434/v1/tools/llm_chat/call \
  -H "Content-Type: application/json" \
  -d "{\"arguments\":{\"prompt\":\"说一个手机本地 AI 节点的实用场景。\",\"maxTokens\":128}}"
```

如果当前模型是 VLM，也可以传 `image` 或 `images` data URL：

```json
{
  "arguments": {
    "prompt": "这张图里有什么？",
    "image": "data:image/jpeg;base64,...",
    "maxTokens": 128
  }
}
```

如果图片来自手机摄像头，先启动 vision，然后直接让 `llm_chat` 读取当前帧：

```json
{
  "arguments": {
    "prompt": "手机摄像头现在看到了什么？",
    "useCameraFrame": true,
    "maxTokens": 128
  }
}
```

不要把 MCP `camera_capture` 文本输出里的占位符再传给 `llm_chat`；大媒体在 MCP 结构化结果里会被压缩。

使用 OpenAI 风格 tool calling：

```json
{
  "model": "qwen3.5-2b-mnn",
  "messages": [
    { "role": "user", "content": "Check the phone runtime status." }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "runtime_status",
        "description": "Return API/runtime status.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    }
  ],
  "tool_choice": {
    "type": "function",
    "function": { "name": "runtime_status" }
  }
}
```

`tool_choice: "auto"` 由模型和 template 决定。如果模型包没有支持 tools 的 chat template，Lociant 不会凭空生成可靠的工具参数。

只有当你希望 Lociant 执行选中的本地工具，并把结果回填给模型时，才设置 `execute_tools: true`。`read`、`edit`、`bash`、`grep` 这类客户端自有工具应该由客户端执行。

## Agent Probe

基础测试：

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --chat
```

测试 MCP 调用手机本地 LLM：

```bash
python scripts/lociant_test.py quick --base-url http://<phone-ip>:11434 --mcp-llm
```

日志代理：

```bash
python scripts/lociant_test.py proxy --base-url http://<phone-ip>:11434/v1 --port 11435
```

把 agent 指向：

```text
http://127.0.0.1:11435/v1
```

代理会显示请求形态、工具 schema、流式行为、延迟，以及 Lociant 是否返回了 `tool_calls`。

## 实用默认值

对手机侧小模型建议：

- 使用 `stream: true`
- `max_tokens` 保持克制，通常 `128` 到 `512`
- 工具 schema 保持简短
- 避免把巨大的工作区文件塞给手机模型
- 让桌面 agent 执行桌面工具
- 用 Lociant 做本地模型调用和手机侧感知/动作
