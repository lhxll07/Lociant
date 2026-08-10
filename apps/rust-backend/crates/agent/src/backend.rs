//! Chat backend abstraction: the agent loop is backend-agnostic. The cloud
//! implementation proxies to a configured OpenAI-compatible endpoint; local
//! inference (llama.cpp / RKLLM) will implement the same trait later.

use futures_util::StreamExt;
use lociant_core::ModelToolCall;
use serde_json::Value;
use tokio::sync::mpsc;

#[derive(Debug, Clone)]
pub enum TurnEvent {
    Chunk(String),
    Reasoning(String),
    ToolCall(ModelToolCall),
}

#[derive(Debug, Clone, Default)]
pub struct TurnOutcome {
    pub ok: bool,
    pub retryable: bool,
    pub message: String,
    pub text: String,
    pub reasoning: String,
    pub tool_calls: Vec<ModelToolCall>,
    pub usage: Option<Value>,
    pub aborted: bool,
}

#[async_trait::async_trait]
pub trait ChatBackend: Send + Sync {
    /// Runs one model turn, streaming content/reasoning/tool-call events into
    /// `events` as they arrive. The sender is dropped before returning.
    async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome;

    /// Runs one model turn without streaming.
    async fn complete_turn(&self, body: &Value) -> TurnOutcome;
}

/// OpenAI-compatible cloud endpoint. Mirrors the Android `CloudChatClient`:
/// `base_url + "/chat/completions"`, bearer auth, standard SSE parsing.
pub struct CloudBackend {
    pub client: reqwest::Client,
    pub base_url: String,
    pub api_key: String,
    pub model: String,
}

#[async_trait::async_trait]
impl ChatBackend for CloudBackend {
    async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome {
        let response = match self.send(body).await {
            Ok(response) => response,
            Err(outcome) => return outcome,
        };
        let status = response.status();
        if !status.is_success() {
            let text = response.text().await.unwrap_or_default();
            return TurnOutcome {
                ok: false,
                retryable: status.is_server_error() || status.as_u16() == 429,
                message: format!(
                    "upstream {status}: {}",
                    text.chars().take(400).collect::<String>()
                ),
                ..TurnOutcome::default()
            };
        }
        parse_stream(response.bytes_stream(), events).await
    }

    async fn complete_turn(&self, body: &Value) -> TurnOutcome {
        let response = match self.send(body).await {
            Ok(response) => response,
            Err(outcome) => return outcome,
        };
        let status = response.status();
        if !status.is_success() {
            let text = response.text().await.unwrap_or_default();
            return TurnOutcome {
                ok: false,
                retryable: status.is_server_error() || status.as_u16() == 429,
                message: format!(
                    "upstream {status}: {}",
                    text.chars().take(400).collect::<String>()
                ),
                ..TurnOutcome::default()
            };
        }
        match response.json::<Value>().await {
            Ok(parsed) => parse_complete(&parsed),
            Err(error) => TurnOutcome {
                ok: false,
                retryable: false,
                message: format!("upstream response parse failed: {error}"),
                ..TurnOutcome::default()
            },
        }
    }
}

impl CloudBackend {
    async fn send(&self, body: &Value) -> Result<reqwest::Response, TurnOutcome> {
        let url = format!("{}/chat/completions", self.base_url.trim_end_matches('/'));
        let mut request = self.client.post(&url);
        if !self.api_key.is_empty() {
            request = request.bearer_auth(&self.api_key);
        }
        request
            .header(reqwest::header::CONTENT_TYPE, "application/json")
            .body(body.to_string())
            .send()
            .await
            .map_err(|error| TurnOutcome {
                ok: false,
                retryable: true,
                message: format!("upstream request failed: {error}"),
                ..TurnOutcome::default()
            })
    }
}

async fn parse_stream(
    mut stream: impl futures_util::Stream<Item = Result<bytes::Bytes, reqwest::Error>> + Unpin,
    events: mpsc::Sender<TurnEvent>,
) -> TurnOutcome {
    let mut line_buf = String::new();
    let mut text = String::new();
    let mut reasoning = String::new();
    let mut calls: Vec<(usize, String, String, String)> = Vec::new();
    let mut usage = None;
    let mut aborted = false;

    while let Some(chunk) = stream.next().await {
        let chunk = match chunk {
            Ok(chunk) => chunk,
            Err(error) => {
                return TurnOutcome {
                    ok: false,
                    retryable: true,
                    message: format!("upstream stream failed: {error}"),
                    text,
                    reasoning,
                    ..TurnOutcome::default()
                };
            }
        };
        line_buf.push_str(&String::from_utf8_lossy(&chunk));
        while let Some(line_end) = line_buf.find('\n') {
            let line = line_buf[..line_end].trim().to_owned();
            line_buf.drain(..=line_end);
            if !line.starts_with("data:") {
                continue;
            }
            let data = line["data:".len()..].trim();
            if data.is_empty() || data == "[DONE]" {
                continue;
            }
            let event: Value = match serde_json::from_str(data) {
                Ok(event) => event,
                Err(_) => continue,
            };
            if let Some(usage_json) = event.get("usage") {
                usage = Some(usage_json.clone());
            }
            let Some(delta) = event.pointer("/choices/0/delta") else {
                continue;
            };
            if let Some(content) = delta.get("content").and_then(Value::as_str) {
                if !content.is_empty() {
                    text.push_str(content);
                    if events
                        .send(TurnEvent::Chunk(content.to_owned()))
                        .await
                        .is_err()
                    {
                        aborted = true;
                        break;
                    }
                }
            }
            if let Some(reason) = delta.get("reasoning_content").and_then(Value::as_str) {
                if !reason.is_empty() {
                    reasoning.push_str(reason);
                    if events
                        .send(TurnEvent::Reasoning(reason.to_owned()))
                        .await
                        .is_err()
                    {
                        aborted = true;
                        break;
                    }
                }
            }
            if let Some(raw_calls) = delta.get("tool_calls").and_then(Value::as_array) {
                for raw in raw_calls {
                    let Some(function) = raw.get("function") else {
                        continue;
                    };
                    let index = raw.get("index").and_then(Value::as_u64).unwrap_or(0) as usize;
                    let id = raw
                        .get("id")
                        .and_then(Value::as_str)
                        .unwrap_or("")
                        .to_owned();
                    let name = function
                        .get("name")
                        .and_then(Value::as_str)
                        .unwrap_or("")
                        .to_owned();
                    let arguments = function
                        .get("arguments")
                        .and_then(Value::as_str)
                        .unwrap_or("");
                    if let Some(slot) = calls.iter_mut().find(|(i, _, _, _)| *i == index) {
                        if !id.is_empty() {
                            slot.1 = id;
                        }
                        slot.2.push_str(&name);
                        slot.3.push_str(arguments);
                    } else {
                        calls.push((index, id, name, arguments.to_owned()));
                    }
                }
            }
        }
        if aborted {
            break;
        }
    }

    let tool_calls = calls
        .into_iter()
        .map(|(_, id, name, arguments)| ModelToolCall {
            id: if id.is_empty() {
                format!(
                    "call_{}",
                    std::time::SystemTime::now()
                        .duration_since(std::time::UNIX_EPOCH)
                        .map(|d| d.as_nanos())
                        .unwrap_or(0)
                )
            } else {
                id
            },
            name,
            arguments: if arguments.is_empty() {
                "{}"
            } else {
                &arguments
            }
            .to_owned(),
        })
        .collect::<Vec<_>>();
    for call in &tool_calls {
        if events
            .send(TurnEvent::ToolCall(call.clone()))
            .await
            .is_err()
        {
            aborted = true;
            break;
        }
    }

    TurnOutcome {
        ok: true,
        text,
        reasoning,
        tool_calls,
        usage,
        aborted,
        ..TurnOutcome::default()
    }
}

fn parse_complete(parsed: &Value) -> TurnOutcome {
    let mut text = String::new();
    let mut reasoning = String::new();
    let mut tool_calls = Vec::new();
    let mut usage = None;
    if let Some(usage_json) = parsed.get("usage") {
        usage = Some(usage_json.clone());
    }
    if let Some(choices) = parsed.get("choices").and_then(Value::as_array) {
        if let Some(message) = choices.first().and_then(|c| c.get("message")) {
            if let Some(content) = message.get("content").and_then(Value::as_str) {
                text.push_str(content);
            }
            if let Some(reason) = message
                .get("reasoning_content")
                .or_else(|| message.get("reasoning"))
                .and_then(Value::as_str)
            {
                reasoning.push_str(reason);
            }
            if let Some(raw_calls) = message.get("tool_calls").and_then(Value::as_array) {
                for raw in raw_calls {
                    let function = raw.get("function");
                    tool_calls.push(ModelToolCall {
                        id: raw
                            .get("id")
                            .and_then(Value::as_str)
                            .unwrap_or("")
                            .to_owned(),
                        name: function
                            .and_then(|f| f.get("name"))
                            .and_then(Value::as_str)
                            .unwrap_or("")
                            .to_owned(),
                        arguments: function
                            .and_then(|f| f.get("arguments"))
                            .and_then(Value::as_str)
                            .unwrap_or("{}")
                            .to_owned(),
                    });
                }
            }
        }
    }
    TurnOutcome {
        ok: true,
        text,
        reasoning,
        tool_calls,
        usage,
        ..TurnOutcome::default()
    }
}
