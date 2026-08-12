//! Rust agent loop: the multi-round model<->tool orchestration previously
//! owned by the Android `ChatController`. It mirrors that behavior exactly —
//! same SSE events (`lociant` phase events, content/reasoning/tool_calls
//! deltas, `[DONE]`), same retry/loop-guard/round-limit rules — so the
//! Flutter UI parses the stream unchanged.

pub mod backend;

use std::collections::VecDeque;
use std::sync::Arc;
use std::time::Duration;

use backend::{ChatBackend, TurnEvent};
use bytes::Bytes;
use lociant_core::{ModelToolCall, ToolDescriptor};
use lociant_tools::ToolRegistry;
use serde_json::{json, Map, Value};
use tokio::sync::mpsc;

pub const ROUNDS_DEFAULT: u32 = 32;
pub const ROUNDS_MIN: u32 = 8;
pub const ROUNDS_MAX: u32 = 64;
pub const MAX_TOOL_CALLS: usize = 64;
pub const MAX_TRANSIENT_RETRIES: u32 = 1;
pub const TOOL_TIMEOUT: Duration = Duration::from_secs(60);
pub const TOOL_RESULT_MAX_CHARS: usize = 8000;
/// Simple message cap for agent context; token-based trimming can replace
/// this when local inference lands.
pub const AGENT_HISTORY_LIMIT: usize = 64;

#[derive(Debug, Clone)]
pub struct StreamMeta {
    pub id: String,
    pub created: i64,
    pub model: String,
}

impl StreamMeta {
    pub fn new(model: &str) -> Self {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis() as i64)
            .unwrap_or(0);
        StreamMeta {
            id: format!("chatcmpl_lociant_{now}"),
            created: now / 1000,
            model: model.to_owned(),
        }
    }
}

#[derive(Debug, Clone)]
pub struct AgentResult {
    pub ok: bool,
    pub text: String,
    pub reasoning: String,
    pub tool_calls: Vec<ModelToolCall>,
    pub error: Option<String>,
    pub usage: Option<Value>,
    pub aborted: bool,
}

impl AgentResult {
    fn error(message: impl Into<String>) -> Self {
        AgentResult {
            ok: false,
            text: String::new(),
            reasoning: String::new(),
            tool_calls: Vec::new(),
            error: Some(message.into()),
            usage: None,
            aborted: false,
        }
    }
}

/// One message inside the loop's context. Content stays a JSON value so image
/// parts and tool results pass through to upstream providers untouched.
#[derive(Debug, Clone)]
pub struct LoopMessage {
    pub role: String,
    pub content: Value,
    pub reasoning: Option<String>,
    pub tool_calls: Vec<ModelToolCall>,
    pub tool_call_id: Option<String>,
    pub name: Option<String>,
}

impl LoopMessage {
    pub fn user(content: Value) -> Self {
        LoopMessage {
            role: "user".into(),
            content,
            reasoning: None,
            tool_calls: Vec::new(),
            tool_call_id: None,
            name: None,
        }
    }

    pub fn assistant(text: &str) -> Self {
        LoopMessage {
            role: "assistant".into(),
            content: json!(text),
            reasoning: None,
            tool_calls: Vec::new(),
            tool_call_id: None,
            name: None,
        }
    }

    pub fn assistant_tools(calls: Vec<ModelToolCall>, reasoning: Option<String>) -> Self {
        LoopMessage {
            role: "assistant".into(),
            content: Value::Null,
            reasoning,
            tool_calls: calls,
            tool_call_id: None,
            name: None,
        }
    }

    pub fn tool(tool_call_id: String, name: String, content: String) -> Self {
        LoopMessage {
            role: "tool".into(),
            content: json!(content),
            reasoning: None,
            tool_calls: Vec::new(),
            tool_call_id: Some(tool_call_id),
            name: Some(name),
        }
    }

    pub fn to_upstream(&self) -> Value {
        let mut message = json!({ "role": self.role, "content": self.content });
        if !self.tool_calls.is_empty() {
            message["content"] = Value::Null;
            message["tool_calls"] = json!(self
                .tool_calls
                .iter()
                .enumerate()
                .map(|(index, call)| openai_tool_call(call, index))
                .collect::<Vec<_>>());
        }
        // Thinking-mode providers require the assistant's reasoning to be
        // passed back verbatim on the next request.
        if let Some(reasoning) = &self.reasoning {
            message["reasoning_content"] = json!(reasoning);
        }
        if let Some(tool_call_id) = &self.tool_call_id {
            message["tool_call_id"] = json!(tool_call_id);
        }
        if let Some(name) = &self.name {
            message["name"] = json!(name);
        }
        message
    }
}

pub fn openai_tool_call(call: &ModelToolCall, index: usize) -> Value {
    json!({
        "index": index,
        "id": call.id,
        "type": "function",
        "function": {
            "name": call.name,
            "arguments": if call.arguments.is_empty() { "{}" } else { &call.arguments },
        },
    })
}

fn openai_tool_definition(tool: &ToolDescriptor) -> Value {
    json!({
        "type": "function",
        "function": {
            "name": tool.name,
            "description": tool.description,
            "parameters": tool.arguments,
        },
    })
}

// ---- SSE rendering (matches the Android event stream byte-for-byte) ----

pub fn sse_start(meta: &StreamMeta) -> String {
    chunk(meta, &[("role", "assistant")], None)
}

pub fn sse_chunk(meta: &StreamMeta, text: &str) -> String {
    chunk(meta, &[("content", text)], None)
}

pub fn sse_reasoning(meta: &StreamMeta, text: &str) -> String {
    chunk(meta, &[("reasoning_content", text)], None)
}

pub fn sse_tool_calls(meta: &StreamMeta, calls: &[ModelToolCall]) -> String {
    let list = calls
        .iter()
        .enumerate()
        .map(|(index, call)| openai_tool_call(call, index))
        .collect::<Vec<_>>();
    let mut delta = Map::new();
    delta.insert("tool_calls".into(), json!(list));
    chunk_delta(meta, delta, None)
}

pub fn sse_phase(meta: &StreamMeta, phase: &str, tool: &str, round: u32, message: &str) -> String {
    let mut lociant = Map::new();
    lociant.insert("type".into(), json!("phase"));
    lociant.insert("phase".into(), json!(phase));
    if !tool.is_empty() {
        lociant.insert("tool".into(), json!(tool));
    }
    if round > 0 {
        lociant.insert("round".into(), json!(round));
    }
    if !message.is_empty() {
        lociant.insert("message".into(), json!(message));
    }
    frame(meta, json!({ "choices": [], "lociant": lociant }))
}

pub fn sse_error(message: &str) -> String {
    format!(
        "data: {}\n\n",
        json!({
            "error": {
                "message": message,
                "type": "invalid_request_error",
                "code": "chat_failed",
            }
        })
    )
}

pub fn sse_finish(meta: &StreamMeta, reason: &str, usage: Option<Value>) -> String {
    let mut out = chunk(meta, &[], Some(reason));
    if let Some(usage) = usage {
        out.push_str(&format!(
            "data: {}\n\n",
            json!({
                "id": meta.id,
                "object": "chat.completion.chunk",
                "created": meta.created,
                "model": meta.model,
                "choices": [],
                "usage": usage,
            })
        ));
    }
    out.push_str("data: [DONE]\n\n");
    out
}

fn chunk(meta: &StreamMeta, entries: &[(&str, &str)], finish: Option<&str>) -> String {
    let mut delta = Map::new();
    for (key, value) in entries {
        delta.insert((*key).into(), json!(value));
    }
    chunk_delta(meta, delta, finish)
}

fn chunk_delta(meta: &StreamMeta, delta: Map<String, Value>, finish: Option<&str>) -> String {
    frame(
        meta,
        json!({
            "choices": [{
                "index": 0,
                "delta": delta,
                "finish_reason": finish.map(|s| json!(s)).unwrap_or(Value::Null),
            }],
        }),
    )
}

fn frame(meta: &StreamMeta, body: Value) -> String {
    let mut base = json!({
        "id": meta.id,
        "object": "chat.completion.chunk",
        "created": meta.created,
        "model": meta.model,
    });
    if let (Some(base_obj), Some(body_obj)) = (base.as_object_mut(), body.as_object()) {
        for (key, value) in body_obj {
            base_obj.insert(key.clone(), value.clone());
        }
    }
    format!("data: {base}\n\n")
}

// ---- loop guard (mirrors the Android ToolLoopGuard) ----

/// Detects a tool doom loop: only a short consecutive run of the exact same
/// tool input counts. Arguments are canonicalized (JSON keys sorted) so
/// providers cannot evade the guard by reordering fields.
pub struct ToolLoopGuard {
    recent: VecDeque<String>,
    threshold: usize,
}

impl ToolLoopGuard {
    pub fn new(threshold: usize) -> Self {
        ToolLoopGuard {
            recent: VecDeque::with_capacity(threshold),
            threshold: threshold.max(1),
        }
    }

    pub fn observe(&mut self, call: &ModelToolCall) -> bool {
        let key = format!("{}\u{0}{}", call.name, canonical_arguments(&call.arguments));
        self.recent.push_back(key);
        while self.recent.len() > self.threshold {
            self.recent.pop_front();
        }
        self.recent.len() == self.threshold && self.recent.iter().all(|k| k == &self.recent[0])
    }
}

fn canonical_arguments(raw: &str) -> String {
    match serde_json::from_str::<Value>(if raw.is_empty() { "{}" } else { raw }) {
        Ok(value) => canonical_value(&value),
        Err(_) => raw.trim().to_owned(),
    }
}

fn canonical_value(value: &Value) -> String {
    match value {
        Value::Object(map) => {
            let mut keys = map.keys().collect::<Vec<_>>();
            keys.sort();
            let inner = keys
                .iter()
                .map(|key| format!("\"{key}\":{}", canonical_value(&map[*key])))
                .collect::<Vec<_>>()
                .join(",");
            format!("{{{inner}}}")
        }
        Value::Array(items) => {
            let inner = items
                .iter()
                .map(canonical_value)
                .collect::<Vec<_>>()
                .join(",");
            format!("[{inner}]")
        }
        Value::String(text) => format!("{text:?}"),
        other => other.to_string(),
    }
}

// ---- agent loop ----

/// Immutable inputs for one agent run, kept together so the loop entry points
/// stay small as options grow.
#[derive(Debug, Clone)]
pub struct AgentConfig {
    pub exposure: String,
    pub model: String,
    pub request_tools: Option<Value>,
    pub execute_tools: bool,
    /// Local models (MNN via IPC, future RKLLM) have tight token budgets:
    /// tool definitions are skipped by default and only re-enabled with an
    /// explicit opt-in later.
    pub local_model: bool,
    /// Pass `enable_thinking` to the upstream (RKLLM server extension for
    /// thinking models); only sent when explicitly enabled.
    pub enable_thinking: bool,
    pub max_rounds: u32,
    pub include_usage: bool,
    pub max_tokens: Option<u32>,
}

struct RunContext<'a> {
    config: &'a AgentConfig,
    tools: &'a [Value],
    streaming: bool,
    meta: Option<&'a StreamMeta>,
    tx: Option<&'a mpsc::Sender<Bytes>>,
}

/// Streams a full multi-round agent run over the SSE channel. Returns the
/// final result so the caller can persist the assistant turn.
pub async fn stream_agent(
    backend: Arc<dyn ChatBackend>,
    registry: Arc<ToolRegistry>,
    config: AgentConfig,
    messages: Vec<LoopMessage>,
    tx: &mpsc::Sender<Bytes>,
) -> AgentResult {
    let meta = StreamMeta::new(&config.model);
    if tx.send(Bytes::from(sse_start(&meta))).await.is_err() {
        return AgentResult {
            aborted: true,
            ..AgentResult::error("client disconnected")
        };
    }
    // Tool metadata is fixed for the whole run; building it once here (with
    // adapter-level caching) keeps the per-round path free of network I/O.
    let tools = build_tools(&config, &registry);
    let context = RunContext {
        config: &config,
        tools: &tools,
        streaming: true,
        meta: Some(&meta),
        tx: Some(tx),
    };
    let result = run_loop(backend, registry, messages, context).await;
    let reason = if result.tool_calls.is_empty() {
        "stop"
    } else {
        "tool_calls"
    };
    let usage = if config.include_usage {
        result.usage.clone()
    } else {
        None
    };
    if tx
        .send(Bytes::from(sse_finish(&meta, reason, usage)))
        .await
        .is_err()
    {
        return AgentResult {
            aborted: true,
            ..result
        };
    }
    result
}

/// Runs the same loop without streaming; used for non-stream chat requests.
pub async fn run_agent_complete(
    backend: Arc<dyn ChatBackend>,
    registry: Arc<ToolRegistry>,
    config: AgentConfig,
    messages: Vec<LoopMessage>,
) -> AgentResult {
    let tools = build_tools(&config, &registry);
    let context = RunContext {
        config: &config,
        tools: &tools,
        streaming: false,
        meta: None,
        tx: None,
    };
    run_loop(backend, registry, messages, context).await
}

/// Builds the OpenAI tool definitions once per agent run. The registry is
/// authoritative when tool execution is enabled; otherwise the caller's
/// request tools pass through untouched.
fn build_tools(config: &AgentConfig, registry: &ToolRegistry) -> Vec<Value> {
    if config.local_model {
        Vec::new()
    } else if config.execute_tools {
        registry
            .visible(&config.exposure)
            .iter()
            .map(openai_tool_definition)
            .collect()
    } else {
        config
            .request_tools
            .as_ref()
            .and_then(Value::as_array)
            .cloned()
            .unwrap_or_default()
    }
}

async fn run_loop(
    backend: Arc<dyn ChatBackend>,
    registry: Arc<ToolRegistry>,
    mut messages: Vec<LoopMessage>,
    context: RunContext<'_>,
) -> AgentResult {
    let RunContext {
        config,
        tools,
        streaming,
        meta,
        tx,
    } = context;
    let mut rounds: u32 = 0;
    let mut retries: u32 = 0;
    let mut executed_tool_calls = 0usize;
    let mut loop_guard = ToolLoopGuard::new(3);

    loop {
        if let (Some(meta), Some(tx)) = (meta, tx) {
            if !send_line(tx, sse_phase(meta, "round", "", rounds, "")).await {
                return AgentResult {
                    aborted: true,
                    ..AgentResult::error("client disconnected")
                };
            }
        }

        let mut body = json!({
            "model": config.model,
            "messages": messages.iter().map(LoopMessage::to_upstream).collect::<Vec<_>>(),
            "stream": streaming,
        });
        if let Some(max_tokens) = config.max_tokens {
            body["max_tokens"] = json!(max_tokens);
        }
        if config.enable_thinking {
            body["enable_thinking"] = json!(true);
        }
        if !tools.is_empty() {
            body["tools"] = json!(tools);
        }

        let outcome = if streaming {
            stream_turn(
                backend.clone(),
                &body,
                meta.expect("stream meta"),
                tx.expect("stream tx"),
            )
            .await
        } else {
            backend.complete_turn(&body).await
        };
        if outcome.aborted {
            tracing::debug!(round = rounds, "agent turn aborted");
            return AgentResult {
                aborted: true,
                ..AgentResult::error("client disconnected")
            };
        }
        tracing::debug!(
            round = rounds,
            ok = outcome.ok,
            retryable = outcome.retryable,
            tool_calls = outcome.tool_calls.len(),
            execute_tools = config.execute_tools,
            "agent turn complete"
        );
        let usage = outcome.usage.clone();

        if !outcome.ok {
            if outcome.retryable && retries < MAX_TRANSIENT_RETRIES {
                retries += 1;
                if let (Some(meta), Some(tx)) = (meta, tx) {
                    if !send_line(tx, sse_phase(meta, "retry", "", rounds, &outcome.message)).await
                    {
                        return AgentResult {
                            aborted: true,
                            ..AgentResult::error("client disconnected")
                        };
                    }
                }
                continue;
            }
            let message = outcome.message;
            if let (Some(_), Some(tx)) = (meta, tx) {
                let _ = send_line(tx, sse_error(&message)).await;
            }
            return AgentResult {
                error: Some(message),
                usage,
                ..AgentResult::error("")
            };
        }

        if outcome.text.is_empty() && outcome.reasoning.is_empty() && outcome.tool_calls.is_empty()
        {
            if retries < MAX_TRANSIENT_RETRIES {
                retries += 1;
                let message = "model returned an empty response";
                if let (Some(meta), Some(tx)) = (meta, tx) {
                    if !send_line(tx, sse_phase(meta, "retry", "", rounds, message)).await {
                        return AgentResult {
                            aborted: true,
                            ..AgentResult::error("client disconnected")
                        };
                    }
                }
                continue;
            }
            let message = "model returned an empty response";
            if let (Some(_), Some(tx)) = (meta, tx) {
                let _ = send_line(tx, sse_error(message)).await;
            }
            return AgentResult {
                error: Some(message.into()),
                usage,
                ..AgentResult::error("")
            };
        }

        if outcome.tool_calls.is_empty() || !config.execute_tools {
            return AgentResult {
                ok: true,
                text: outcome.text,
                reasoning: outcome.reasoning,
                tool_calls: outcome.tool_calls,
                error: None,
                usage,
                ..AgentResult::error("")
            };
        }

        if rounds >= config.max_rounds {
            let message = format!(
                "reached the tool-round limit ({}); the task may be incomplete",
                config.max_rounds
            );
            if let (Some(_), Some(tx)) = (meta, tx) {
                let _ = send_line(tx, sse_error(&message)).await;
            }
            return AgentResult {
                error: Some(message),
                usage,
                ..AgentResult::error("")
            };
        }
        if let Some(repeated) = outcome
            .tool_calls
            .iter()
            .find(|call| loop_guard.observe(call))
        {
            let message = format!("stopped after repeated tool call: {}", repeated.name);
            if let (Some(_), Some(tx)) = (meta, tx) {
                let _ = send_line(tx, sse_error(&message)).await;
            }
            return AgentResult {
                error: Some(message),
                usage,
                ..AgentResult::error("")
            };
        }
        if executed_tool_calls + outcome.tool_calls.len() > MAX_TOOL_CALLS {
            let message = format!(
                "reached the tool-call limit ({MAX_TOOL_CALLS}); the task may be incomplete"
            );
            if let (Some(_), Some(tx)) = (meta, tx) {
                let _ = send_line(tx, sse_error(&message)).await;
            }
            return AgentResult {
                error: Some(message),
                usage,
                ..AgentResult::error("")
            };
        }

        messages.push(LoopMessage::assistant_tools(
            outcome.tool_calls.clone(),
            if outcome.reasoning.is_empty() {
                None
            } else {
                Some(outcome.reasoning.clone())
            },
        ));
        for call in &outcome.tool_calls {
            if let (Some(meta), Some(tx)) = (meta, tx) {
                if !send_line(tx, sse_phase(meta, "tool_running", &call.name, rounds, "")).await {
                    return AgentResult {
                        aborted: true,
                        ..AgentResult::error("client disconnected")
                    };
                }
            }
            let result = run_tool_bounded(&registry, &config.exposure, call).await;
            let content = truncate(&result.to_string(), TOOL_RESULT_MAX_CHARS);
            messages.push(LoopMessage::tool(
                call.id.clone(),
                call.name.clone(),
                content,
            ));
        }
        if let (Some(meta), Some(tx)) = (meta, tx) {
            if !send_line(tx, sse_phase(meta, "tool_done", "", rounds, "")).await {
                return AgentResult {
                    aborted: true,
                    ..AgentResult::error("client disconnected")
                };
            }
        }
        executed_tool_calls += outcome.tool_calls.len();
        messages = trim_messages(messages, AGENT_HISTORY_LIMIT);
        rounds += 1;
    }
}

async fn stream_turn(
    backend: Arc<dyn ChatBackend>,
    body: &Value,
    meta: &StreamMeta,
    tx: &mpsc::Sender<Bytes>,
) -> backend::TurnOutcome {
    let (event_tx, mut event_rx) = mpsc::channel(64);
    let body = body.clone();
    let handle = tokio::spawn(async move { backend.stream_turn(&body, event_tx).await });
    let mut aborted = false;
    while let Some(event) = event_rx.recv().await {
        let line = match event {
            TurnEvent::Chunk(text) => sse_chunk(meta, &text),
            TurnEvent::Reasoning(text) => sse_reasoning(meta, &text),
            TurnEvent::ToolCall(call) => sse_tool_calls(meta, std::slice::from_ref(&call)),
        };
        if tx.send(Bytes::from(line)).await.is_err() {
            aborted = true;
            break;
        }
    }
    let mut outcome = match handle.await {
        Ok(outcome) => outcome,
        Err(error) => {
            return backend::TurnOutcome {
                ok: false,
                retryable: false,
                message: format!("backend task failed: {error}"),
                aborted,
                ..backend::TurnOutcome::default()
            };
        }
    };
    outcome.aborted = aborted || outcome.aborted;
    outcome
}

async fn send_line(tx: &mpsc::Sender<Bytes>, line: String) -> bool {
    tx.send(Bytes::from(line)).await.is_ok()
}

async fn run_tool_bounded(
    registry: &Arc<ToolRegistry>,
    exposure: &str,
    call: &ModelToolCall,
) -> Value {
    let registry = registry.clone();
    let name = call.name.clone();
    let arguments = serde_json::from_str(&call.arguments).unwrap_or(Value::Null);
    let exposure = exposure.to_owned();
    let result = tokio::time::timeout(
        TOOL_TIMEOUT,
        tokio::task::spawn_blocking(move || registry.call_local(&name, arguments, &exposure)),
    )
    .await;
    match result {
        Ok(Ok(Ok(tool_result))) => json!({
            "ok": true,
            "tool_call_id": call.id,
            "content": tool_result.content,
            "structured": tool_result.structured,
        }),
        Ok(Ok(Err(error))) => json!({
            "ok": false,
            "tool_call_id": call.id,
            "error": { "code": "tool_failed", "message": error.to_string() },
        }),
        Ok(Err(error)) => json!({
            "ok": false,
            "tool_call_id": call.id,
            "error": { "code": "tool_failed", "message": error.to_string() },
        }),
        Err(_) => json!({
            "ok": false,
            "tool_call_id": call.id,
            "error": {
                "code": "tool_timeout",
                "message": format!("tool {} timed out after 60s", call.name),
            },
        }),
    }
}

fn truncate(text: &str, max: usize) -> String {
    let mut out = text.chars().take(max).collect::<String>();
    if out.len() < text.len() {
        out.push_str("\n...(tool result truncated by Lociant)");
    }
    out
}

/// Cheap history trim: keep leading system messages and the newest
/// `limit` messages. Token-based trimming can replace this later.
fn trim_messages(messages: Vec<LoopMessage>, limit: usize) -> Vec<LoopMessage> {
    if messages.len() <= limit {
        return messages;
    }
    let system = messages
        .iter()
        .filter(|m| m.role == "system")
        .cloned()
        .collect::<Vec<_>>();
    let non_system = messages
        .iter()
        .filter(|m| m.role != "system")
        .skip(messages.len().saturating_sub(limit))
        .cloned()
        .collect::<Vec<_>>();
    system.into_iter().chain(non_system).collect()
}

/// OpenAI non-streaming response for a finished agent run.
pub fn complete_response(model: &str, result: &AgentResult, session_id: &str) -> Value {
    if !result.ok {
        return json!({
            "error": {
                "message": result.error.clone().unwrap_or_else(|| "chat failed".into()),
                "type": "invalid_request_error",
                "code": "chat_failed",
            }
        });
    }
    let usage = result.usage.clone().unwrap_or_else(
        || json!({ "prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0 }),
    );
    let base = json!({
        "id": format!("chatcmpl_lociant_{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_millis())
            .unwrap_or(0)),
        "object": "chat.completion",
        "created": std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0),
        "model": model,
        "choices": [{
            "index": 0,
            "message": {
                "role": "assistant",
                "content": if result.tool_calls.is_empty() { json!(result.text) } else { Value::Null },
                "tool_calls": if result.tool_calls.is_empty() {
                    Value::Null
                } else {
                    json!(result.tool_calls.iter().enumerate().map(|(i, c)| openai_tool_call(c, i)).collect::<Vec<_>>())
                },
            },
            "finish_reason": if result.tool_calls.is_empty() { "stop" } else { "tool_calls" },
        }],
        "usage": usage,
        "sessionId": session_id,
    });
    base
}

#[cfg(test)]
mod tests {
    use super::*;
    use backend::TurnOutcome;
    use lociant_tools::NoopDevice;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::sync::Mutex;

    struct FakeBackend {
        calls: Arc<AtomicUsize>,
        last_body: Arc<Mutex<Option<Value>>>,
    }

    #[async_trait::async_trait]
    impl ChatBackend for FakeBackend {
        async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome {
            let _ = self.calls.fetch_add(1, Ordering::SeqCst);
            *self.last_body.lock().unwrap() = Some(body.clone());
            let has_tool = body["messages"]
                .as_array()
                .map(|msgs| msgs.iter().any(|m| m["role"] == "tool"))
                .unwrap_or(false);
            if has_tool {
                let _ = events.send(TurnEvent::Reasoning("r".into())).await;
                let _ = events.send(TurnEvent::Chunk("done".into())).await;
                TurnOutcome {
                    ok: true,
                    text: "done".into(),
                    reasoning: "r".into(),
                    ..TurnOutcome::default()
                }
            } else {
                let call = ModelToolCall {
                    id: "call_1".into(),
                    name: "tap".into(),
                    arguments: "{}".into(),
                };
                let _ = events.send(TurnEvent::ToolCall(call.clone())).await;
                TurnOutcome {
                    ok: true,
                    tool_calls: vec![call],
                    ..TurnOutcome::default()
                }
            }
        }

        async fn complete_turn(&self, _body: &Value) -> TurnOutcome {
            TurnOutcome::default()
        }
    }

    #[tokio::test]
    async fn agent_loop_runs_tools_across_rounds() {
        let backend: Arc<dyn ChatBackend> = Arc::new(FakeBackend {
            calls: Arc::new(AtomicUsize::new(0)),
            last_body: Arc::new(Mutex::new(None)),
        });
        let registry = Arc::new(ToolRegistry::new(Box::new(NoopDevice)));
        let config = AgentConfig {
            exposure: "action".into(),
            model: "m".into(),
            request_tools: None,
            execute_tools: true,
            local_model: false,
            enable_thinking: false,
            max_rounds: 32,
            include_usage: false,
            max_tokens: None,
        };
        let (tx, mut rx) = mpsc::channel(128);
        let result = stream_agent(
            backend,
            registry,
            config,
            vec![LoopMessage::user(json!("hi"))],
            &tx,
        )
        .await;
        assert!(result.ok, "expected ok, got {:?}", result.error);
        assert_eq!(result.text, "done");
        drop(tx);

        let mut sse = String::new();
        while let Some(bytes) = rx.recv().await {
            sse.push_str(&String::from_utf8_lossy(&bytes));
        }
        assert!(sse.contains("tool_running"), "missing tool_running phase");
        assert!(sse.contains("tool_done"), "missing tool_done phase");
        assert!(sse.contains("\"round\":1"), "missing round 1 phase");
        assert!(
            sse.contains("\"content\":\"done\""),
            "missing final content"
        );
        assert!(sse.contains("[DONE]"), "missing [DONE]");
    }

    #[tokio::test]
    async fn local_models_skip_tool_definitions() {
        let last_body = Arc::new(Mutex::new(None));
        let backend: Arc<dyn ChatBackend> = Arc::new(FakeBackend {
            calls: Arc::new(AtomicUsize::new(0)),
            last_body: last_body.clone(),
        });
        let registry = Arc::new(ToolRegistry::new(Box::new(NoopDevice)));
        let config = AgentConfig {
            exposure: "action".into(),
            model: "local-model".into(),
            request_tools: None,
            execute_tools: true,
            local_model: true,
            enable_thinking: false,
            max_rounds: 32,
            include_usage: false,
            max_tokens: None,
        };
        let (tx, _rx) = mpsc::channel(128);
        let result = stream_agent(
            backend,
            registry,
            config,
            vec![LoopMessage::user(json!("hi"))],
            &tx,
        )
        .await;
        assert!(result.ok, "expected ok, got {:?}", result.error);
        let body = last_body
            .lock()
            .unwrap()
            .clone()
            .expect("backend saw a body");
        assert!(
            body.get("tools").is_none(),
            "local models must not receive tool definitions"
        );
    }

    #[test]
    fn loop_guard_detects_repeats_but_allows_work() {
        let mut guard = ToolLoopGuard::new(3);
        let call = |name: &str, args: &str| ModelToolCall {
            id: "id".into(),
            name: name.into(),
            arguments: args.into(),
        };
        assert!(!guard.observe(&call("tap", r#"{"x":1,"y":2}"#)));
        assert!(!guard.observe(&call("tap", r#"{"y":2,"x":1}"#))); // same canonical args
        assert!(guard.observe(&call("tap", r#"{"x":1,"y":2}"#)));
        assert!(!guard.observe(&call("tap", r#"{"x":3,"y":2}"#))); // different args breaks it
    }

    #[test]
    fn upstream_message_shapes() {
        let user = LoopMessage::user(json!("hello"));
        assert_eq!(user.to_upstream()["role"], "user");
        assert_eq!(user.to_upstream()["content"], "hello");

        let assistant = LoopMessage::assistant_tools(
            vec![ModelToolCall {
                id: "call_1".into(),
                name: "tap".into(),
                arguments: "{}".into(),
            }],
            Some("think".into()),
        );
        let json = assistant.to_upstream();
        assert_eq!(json["content"], Value::Null);
        assert_eq!(json["tool_calls"][0]["function"]["name"], "tap");
        assert_eq!(json["reasoning_content"], "think");

        let tool = LoopMessage::tool("call_1".into(), "tap".into(), "ok".into());
        assert_eq!(tool.to_upstream()["tool_call_id"], "call_1");
        assert_eq!(tool.to_upstream()["content"], "ok");
    }

    #[test]
    fn sse_events_are_parseable() {
        let meta = StreamMeta::new("m");
        let chunk = sse_chunk(&meta, "hi");
        assert!(chunk.starts_with("data: "));
        let value: Value = serde_json::from_str(chunk[6..].trim()).unwrap();
        assert_eq!(value["choices"][0]["delta"]["content"], "hi");

        let phase = sse_phase(&meta, "tool_running", "tap", 1, "");
        let value: Value = serde_json::from_str(phase[6..].trim()).unwrap();
        assert_eq!(value["lociant"]["phase"], "tool_running");
        assert_eq!(value["lociant"]["round"], 1);
    }
}
