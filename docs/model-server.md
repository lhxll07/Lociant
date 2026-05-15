# Model Server Runtime Panel

> Version: 0.4.1 | Updated: 2026-05-15

[English](#english) · [中文](#中文)

## English

`model-server` is the built-in panel for MNNode's LAN model API. It is runtime infrastructure, not a normal third-party Scene Pack.

It turns an Android phone into a local Ollama-like model node backed by MNNode's model manager, MNN runtime, Room sessions, and foreground runtime service.

## Current Status

Working:

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /api/chat`
- text chat
- image chat
- OpenAI SSE streaming
- Ollama NDJSON streaming
- explicit session persistence
- serialized multi-request inference
- process-local text prompt cache
- Runtime Window visible anchor for Android background stability

Defaults:

```text
host: 0.0.0.0
port: 11434
start: manual
concurrency: one model request at a time
background: foreground runtime service + optional Runtime Window
```

## Sessions

Explicit sessions are persisted in Room. Supply a session id through:

- `X-MNNode-Session-Id`
- `X-Session-Id`
- `sessionId`
- `session_id`
- `metadata.sessionId`
- `metadata.session_id`

Anonymous API requests are served but are not durable chat sessions by default.

## Token Policy

Output length is unified as:

```text
min(request value or default 512, runtime hard cap 4096, model config max_new_tokens if present)
```

Supported request fields:

- OpenAI: `max_tokens`
- Ollama: `options.num_predict`
- Scene calls: `maxTokens`

`/health` reports the default, runtime, model, and effective output caps for diagnostics.

## Cache

The current prompt cache is intentionally narrow:

- process-local
- text-only
- explicit session only
- reset on session switch, image input, restart, or incompatible message shape

Room remains the durable conversation source. Cache is only a runtime optimization.

## Visible Runtime

MNNode does not treat Android as a reliable invisible daemon environment. The model server is designed to run as a visible runtime:

- `MNNodeRuntimeService` owns the LAN API lifecycle
- foreground notification remains visible while running
- Runtime Window can show status, model, and LAN URL over other apps
- Runtime Window click opens the app, long press hides it, double click toggles runtime
- boot autostart remains out of scope and opt-in only later

Do not include headless camera or vision in v1.

## Testing

Preferred regression script:

```bash
python scripts/model_server_smoke.py
```

Fast endpoint-only check:

```bash
python scripts/model_server_smoke.py --skip-chat
```

On Windows, prefer `curl.exe --http1.1` over PowerShell web cmdlets when debugging streaming or POST behavior.

## Security

Until auth is implemented:

- keep it LAN-only
- do not expose to the public internet
- keep manual start as the default
- keep one active model request at a time
- show visible foreground-service notification while running
- prefer Runtime Window for long-running inference on vendor ROMs

---

## 中文

`model-server` 是 MNNode 局域网模型 API 的内置面板。它是 runtime 基础设施，不是普通第三方 Scene Pack。

它把 Android 手机变成本地 Ollama-like 模型节点，底层复用 MNNode 的模型管理、MNN runtime、Room 会话和前台 runtime service。

## 当前状态

已可用：

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /api/chat`
- 文本对话
- 图片对话
- OpenAI SSE 流式输出
- Ollama NDJSON 流式输出
- 显式 session 持久化
- 多 request 串行推理
- 进程内文本 prompt cache
- Runtime Window 可见锚点，用于提升 Android 后台稳定性

默认配置：

```text
host: 0.0.0.0
port: 11434
启动: 手动
并发: 同一时间一个模型请求
后台: 前台 runtime service + 可选 Runtime Window
```

## Sessions

显式 session 会持久化到 Room。可通过以下方式传入 session id：

- `X-MNNode-Session-Id`
- `X-Session-Id`
- `sessionId`
- `session_id`
- `metadata.sessionId`
- `metadata.session_id`

匿名 API 请求会正常处理，但默认不作为持久对话 session。

## Token 策略

输出长度统一为：

```text
min(请求值或默认 512, runtime 硬上限 4096, 模型 config 中的 max_new_tokens)
```

支持字段：

- OpenAI: `max_tokens`
- Ollama: `options.num_predict`
- 场景调用: `maxTokens`

`/health` 会返回默认、runtime、模型和最终生效的输出上限，方便诊断。

## Cache

当前 prompt cache 刻意保持收窄：

- 仅进程内
- 仅文本
- 仅显式 session
- 切换 session、图片输入、重启或不兼容消息结构时重置

Room 仍然是持久对话事实源。Cache 只是 runtime 优化。

## 可见 Runtime

MNNode 不把 Android 当成可靠的隐藏后台 daemon 环境。Model server 设计为可见 runtime：

- `MNNodeRuntimeService` 持有局域网 API 生命周期
- 运行时保持前台通知可见
- Runtime Window 可在其他 App 上方显示状态、模型和 LAN URL
- Runtime Window 点击打开 App，长按隐藏，双击启停 runtime
- 开机自启暂不做，以后也只作为 opt-in

v1 不包含 headless camera 或 vision。

## 测试

优先使用回归脚本：

```bash
python scripts/model_server_smoke.py
```

只检查基础端点：

```bash
python scripts/model_server_smoke.py --skip-chat
```

Windows 调试 streaming 或 POST 行为时，优先用 `curl.exe --http1.1`，少用 PowerShell web cmdlets。

## 安全

认证实现前：

- 保持局域网使用
- 不暴露公网
- 默认手动启动
- 同一时间只跑一个模型请求
- 运行时显示前台服务通知
- 在厂商 ROM 上长时间推理时优先使用 Runtime Window
