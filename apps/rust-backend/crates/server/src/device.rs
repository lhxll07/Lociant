//! IPC device adapter: talks to the Kotlin device layer (Android) over a
//! localhost TCP JSON protocol. Rust stays the single policy owner
//! (exposure + remote_allowed in `ToolRegistry`); this side only discovers
//! and executes.

use async_trait::async_trait;
use std::io::{BufRead, BufReader, Write};
use std::net::TcpStream;
use std::time::Duration;

use lociant_agent::backend::{ChatBackend, TurnEvent, TurnOutcome};
use lociant_core::{ToolDescriptor, ToolResult};
use lociant_tools::{DeviceAdapter, ToolError};
use serde_json::{json, Value};
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader as AsyncBufReader};
use tokio::sync::mpsc;

pub struct IpcDeviceAdapter {
    pub port: u16,
    pub token: String,
}

impl IpcDeviceAdapter {
    pub const TOKEN_ENV: &'static str = "LOCIANT_DEVICE_TOKEN";
    pub const PORT_ENV: &'static str = "LOCIANT_DEVICE_PORT";

    fn request(&self, payload: &Value) -> Result<Value, String> {
        let stream = TcpStream::connect(("127.0.0.1", self.port))
            .map_err(|error| format!("device adapter connect failed: {error}"))?;
        stream.set_read_timeout(Some(Duration::from_secs(10))).ok();
        stream.set_write_timeout(Some(Duration::from_secs(5))).ok();
        let mut stream = stream;
        let mut line = payload.to_string();
        line.push('\n');
        stream
            .write_all(line.as_bytes())
            .map_err(|error| format!("device adapter write failed: {error}"))?;
        let mut response = String::new();
        BufReader::new(stream)
            .read_line(&mut response)
            .map_err(|error| format!("device adapter read failed: {error}"))?;
        serde_json::from_str(&response)
            .map_err(|error| format!("device adapter response parse failed: {error}"))
    }

    /// Local MNN model specs from the Kotlin model manager.
    pub fn models(&self) -> Vec<Value> {
        let request = json!({ "token": self.token, "method": "models.list" });
        let Ok(response) = self.request(&request) else {
            return Vec::new();
        };
        if response.get("ok").and_then(Value::as_bool) != Some(true) {
            return Vec::new();
        }
        response
            .get("models")
            .and_then(Value::as_array)
            .cloned()
            .unwrap_or_default()
    }

    pub fn has_model(&self, model_id: &str) -> bool {
        self.models().iter().any(|model| {
            model.get("id").and_then(Value::as_str) == Some(model_id)
                || model.get("name").and_then(Value::as_str) == Some(model_id)
        })
    }
}

impl DeviceAdapter for IpcDeviceAdapter {
    fn tools(&self) -> Vec<ToolDescriptor> {
        let request = json!({ "token": self.token, "method": "tools.list" });
        let Ok(response) = self.request(&request) else {
            return Vec::new();
        };
        if response.get("ok").and_then(Value::as_bool) != Some(true) {
            return Vec::new();
        }
        response
            .get("tools")
            .and_then(Value::as_array)
            .map(|tools| tools.iter().filter_map(kotlin_descriptor).collect())
            .unwrap_or_default()
    }

    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError> {
        let request = json!({
            "token": self.token,
            "method": "tools.call",
            "name": name,
            "arguments": arguments,
        });
        let response = self.request(&request).map_err(ToolError::Adapter)?;
        Ok(map_result(response))
    }
}

/// Runs local MNN turns through the same IPC channel, streaming chunks /
/// reasoning / tool calls back as they are produced by the Kotlin runtime.
pub struct IpcChatBackend {
    pub port: u16,
    pub token: String,
}

#[async_trait]
impl ChatBackend for IpcChatBackend {
    async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome {
        ipc_chat(self, body, Some(events)).await
    }

    async fn complete_turn(&self, body: &Value) -> TurnOutcome {
        ipc_chat(self, body, None).await
    }
}

async fn ipc_chat(
    backend: &IpcChatBackend,
    body: &Value,
    events: Option<mpsc::Sender<TurnEvent>>,
) -> TurnOutcome {
    tokio::time::timeout(Duration::from_secs(600), async {
        let mut stream = match tokio::net::TcpStream::connect(("127.0.0.1", backend.port)).await {
            Ok(stream) => stream,
            Err(error) => {
                return TurnOutcome {
                    ok: false,
                    retryable: true,
                    message: format!("device chat connect failed: {error}"),
                    ..TurnOutcome::default()
                };
            }
        };
        let request = json!({
            "token": backend.token,
            "method": "chat.invoke",
            "request": body,
        });
        let mut line = request.to_string();
        line.push('\n');
        if let Err(error) = stream.write_all(line.as_bytes()).await {
            return TurnOutcome {
                ok: false,
                retryable: true,
                message: format!("device chat write failed: {error}"),
                ..TurnOutcome::default()
            };
        }

        let mut reader = AsyncBufReader::new(stream);
        let mut line_buf = String::new();
        let mut text = String::new();
        let mut reasoning = String::new();
        let mut tool_calls: Vec<lociant_core::ModelToolCall> = Vec::new();
        let mut usage = None;
        loop {
            line_buf.clear();
            match reader.read_line(&mut line_buf).await {
                Ok(0) => break,
                Ok(_) => {}
                Err(error) => {
                    return TurnOutcome {
                        ok: false,
                        retryable: true,
                        message: format!("device chat read failed: {error}"),
                        text,
                        reasoning,
                        tool_calls,
                        ..TurnOutcome::default()
                    };
                }
            }
            let event: Value = match serde_json::from_str(line_buf.trim()) {
                Ok(event) => event,
                Err(_) => continue,
            };
            match event.get("type").and_then(Value::as_str) {
                Some("chunk") => {
                    if let Some(chunk) = event.get("text").and_then(Value::as_str) {
                        if !chunk.is_empty() {
                            text.push_str(chunk);
                            if let Some(events) = &events {
                                if events
                                    .send(TurnEvent::Chunk(chunk.to_owned()))
                                    .await
                                    .is_err()
                                {
                                    return TurnOutcome {
                                        aborted: true,
                                        ..TurnOutcome::default()
                                    };
                                }
                            }
                        }
                    }
                }
                Some("reasoning") => {
                    if let Some(chunk) = event.get("text").and_then(Value::as_str) {
                        if !chunk.is_empty() {
                            reasoning.push_str(chunk);
                            if let Some(events) = &events {
                                if events
                                    .send(TurnEvent::Reasoning(chunk.to_owned()))
                                    .await
                                    .is_err()
                                {
                                    return TurnOutcome {
                                        aborted: true,
                                        ..TurnOutcome::default()
                                    };
                                }
                            }
                        }
                    }
                }
                Some("tool_call") => {
                    let call = lociant_core::ModelToolCall {
                        id: event
                            .get("id")
                            .and_then(Value::as_str)
                            .unwrap_or("")
                            .to_owned(),
                        name: event
                            .get("name")
                            .and_then(Value::as_str)
                            .unwrap_or("")
                            .to_owned(),
                        arguments: event
                            .get("arguments")
                            .and_then(Value::as_str)
                            .unwrap_or("{}")
                            .to_owned(),
                    };
                    if !call.name.is_empty() {
                        tool_calls.push(call.clone());
                        if let Some(events) = &events {
                            if events.send(TurnEvent::ToolCall(call)).await.is_err() {
                                return TurnOutcome {
                                    aborted: true,
                                    ..TurnOutcome::default()
                                };
                            }
                        }
                    }
                }
                Some("done") => {
                    let ok = event.get("ok").and_then(Value::as_bool).unwrap_or(false);
                    if !ok {
                        return TurnOutcome {
                            ok: false,
                            retryable: false,
                            message: event
                                .get("message")
                                .and_then(Value::as_str)
                                .unwrap_or("device chat failed")
                                .to_owned(),
                            text,
                            reasoning,
                            tool_calls,
                            usage,
                            ..TurnOutcome::default()
                        };
                    }
                    let final_text = event
                        .get("text")
                        .and_then(Value::as_str)
                        .map(str::to_owned)
                        .unwrap_or(text);
                    let final_reasoning = event
                        .get("reasoning")
                        .and_then(Value::as_str)
                        .map(str::to_owned)
                        .unwrap_or(reasoning);
                    if let Some(calls) = event.get("toolCalls").and_then(Value::as_array) {
                        if !calls.is_empty() {
                            tool_calls = calls
                                .iter()
                                .map(|call| lociant_core::ModelToolCall {
                                    id: call
                                        .get("id")
                                        .and_then(Value::as_str)
                                        .unwrap_or("")
                                        .to_owned(),
                                    name: call
                                        .get("name")
                                        .and_then(Value::as_str)
                                        .unwrap_or("")
                                        .to_owned(),
                                    arguments: call
                                        .get("arguments")
                                        .and_then(Value::as_str)
                                        .unwrap_or("{}")
                                        .to_owned(),
                                })
                                .collect();
                        }
                    }
                    usage = event.get("usage").cloned().or(usage);
                    return TurnOutcome {
                        ok: true,
                        text: final_text,
                        reasoning: final_reasoning,
                        tool_calls,
                        usage,
                        ..TurnOutcome::default()
                    };
                }
                _ => {}
            }
        }
        TurnOutcome {
            ok: false,
            retryable: true,
            message: "device chat stream ended without done".into(),
            text,
            reasoning,
            tool_calls,
            ..TurnOutcome::default()
        }
    })
    .await
    .unwrap_or_else(|_| TurnOutcome {
        ok: false,
        retryable: true,
        message: "device chat timed out".into(),
        ..TurnOutcome::default()
    })
}

fn kotlin_descriptor(raw: &Value) -> Option<ToolDescriptor> {
    let function = raw.get("function")?;
    let policy = raw.get("x_policy").cloned().unwrap_or_else(|| json!({}));
    let flag = |key: &str| policy.get(key).and_then(Value::as_bool).unwrap_or(false);
    Some(ToolDescriptor {
        name: function
            .get("name")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned(),
        description: function
            .get("description")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned(),
        arguments: function
            .get("parameters")
            .cloned()
            .unwrap_or_else(|| json!({ "type": "object", "properties": {} })),
        exposure: raw
            .get("x_lociant_level")
            .and_then(Value::as_str)
            .unwrap_or("read")
            .to_owned(),
        local: flag("local"),
        remote_allowed: flag("remoteAllowed"),
        requires_activity: flag("requiresActivity"),
        side_effect: flag("sideEffect"),
        destructive: flag("destructive"),
        open_world: flag("openWorld"),
    })
}

fn map_result(response: Value) -> ToolResult {
    let ok = response.get("ok").and_then(Value::as_bool).unwrap_or(false);
    let result = response.get("result").cloned().unwrap_or(Value::Null);
    let content = result
        .get("content")
        .and_then(Value::as_str)
        .map(str::to_owned)
        .unwrap_or_else(|| result.to_string());
    let error = if ok {
        None
    } else {
        Some(
            response
                .get("error")
                .and_then(|error| error.get("message"))
                .and_then(Value::as_str)
                .unwrap_or("tool failed")
                .to_owned(),
        )
    };
    ToolResult {
        ok,
        content,
        structured: result,
        error,
    }
}
