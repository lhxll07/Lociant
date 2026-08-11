//! Local NPU inference backend backed by the Rockchip LLM runtime.
//!
//! The model runs synchronously in a blocking task; generated chunks are
//! forwarded to the agent loop through the same `ChatBackend` events the
//! cloud backend uses, so streaming UI works unchanged. Qwen-style
//! `<think>` blocks are split into `Reasoning` events.

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use lociant_agent::backend::{ChatBackend, TurnEvent, TurnOutcome};
use lociant_rkllm::{Chunk, Rkllm};
use serde_json::Value;
use tokio::sync::mpsc;

pub struct RkllmBackend {
    pub model: Arc<Rkllm>,
    pub enable_thinking: bool,
}

#[async_trait]
impl ChatBackend for RkllmBackend {
    async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome {
        let (system, prompt) = build_prompt(body);
        if prompt.trim().is_empty() {
            return TurnOutcome {
                ok: false,
                retryable: false,
                message: "no user message found in request".to_owned(),
                ..TurnOutcome::default()
            };
        }
        let prompt = if system.trim().is_empty() {
            prompt
        } else {
            format!("{system}\n\n{prompt}")
        };
        let model = self.model.clone();
        let enable_thinking = self.enable_thinking;
        let max_tokens = body
            .get("max_tokens")
            .and_then(Value::as_u64)
            .map(|v| v as i32)
            .filter(|v| *v > 0);

        let abort_model = model.clone();
        let task = tokio::task::spawn_blocking(move || {
            let mut text = String::new();
            let mut reasoning = String::new();
            let mut in_think = false;
            let mut run_error: Option<String> = None;
            let run = model.run(
                &prompt,
                "user",
                enable_thinking,
                max_tokens,
                |chunk| match chunk {
                    Chunk::Text(raw) => {
                        for part in split_thinking(&raw, &mut in_think) {
                            match part {
                                ThinkPart::Reason(r) => {
                                    reasoning.push_str(&r);
                                    if events.blocking_send(TurnEvent::Reasoning(r)).is_err() {
                                        return;
                                    }
                                }
                                ThinkPart::Text(t) => {
                                    text.push_str(&t);
                                    if events.blocking_send(TurnEvent::Chunk(t)).is_err() {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    Chunk::Finished { .. } => {}
                    Chunk::Error(e) => run_error = Some(e),
                },
            );
            match run {
                Ok(()) => TurnOutcome {
                    ok: run_error.is_none(),
                    retryable: false,
                    message: run_error.unwrap_or_default(),
                    text,
                    reasoning,
                    ..TurnOutcome::default()
                },
                Err(error) => TurnOutcome {
                    ok: false,
                    retryable: false,
                    message: format!("rkllm run failed: {error}"),
                    text,
                    reasoning,
                    ..TurnOutcome::default()
                },
            }
        })
        ;
        // RKLLM runs occasionally hang on some inputs; bound the inference so
        // a stuck model never wedges the agent loop, then abort the runtime.
        match tokio::time::timeout(Duration::from_secs(120), task).await {
            Ok(Ok(outcome)) => outcome,
            Ok(Err(join)) => TurnOutcome {
                ok: false,
                retryable: true,
                message: format!("rkllm task failed to join: {join}"),
                ..TurnOutcome::default()
            },
            Err(_) => {
                abort_model.abort();
                TurnOutcome {
                    ok: false,
                    retryable: true,
                    message: "rkllm inference timed out after 120s".to_owned(),
                    ..TurnOutcome::default()
                }
            }
        }
    }

    async fn complete_turn(&self, body: &Value) -> TurnOutcome {
        let (system, prompt) = build_prompt(body);
        if prompt.trim().is_empty() {
            return TurnOutcome {
                ok: false,
                retryable: false,
                message: "no user message found in request".to_owned(),
                ..TurnOutcome::default()
            };
        }
        let prompt = if system.trim().is_empty() {
            prompt
        } else {
            format!("{system}\n\n{prompt}")
        };
        let model = self.model.clone();
        let enable_thinking = self.enable_thinking;
        let max_tokens = body
            .get("max_tokens")
            .and_then(Value::as_u64)
            .map(|v| v as i32)
            .filter(|v| *v > 0);
        let abort_model = model.clone();
        let task = tokio::task::spawn_blocking(move || {
            let mut text = String::new();
            let mut reasoning = String::new();
            let mut in_think = false;
            let mut run_error: Option<String> = None;
            let run = model.run(
                &prompt,
                "user",
                enable_thinking,
                max_tokens,
                |chunk| match chunk {
                    Chunk::Text(raw) => {
                        for part in split_thinking(&raw, &mut in_think) {
                            match part {
                                ThinkPart::Reason(r) => reasoning.push_str(&r),
                                ThinkPart::Text(t) => text.push_str(&t),
                            }
                        }
                    }
                    Chunk::Finished { .. } => {}
                    Chunk::Error(e) => run_error = Some(e),
                },
            );
            match run {
                Ok(()) => TurnOutcome {
                    ok: run_error.is_none(),
                    retryable: false,
                    message: run_error.unwrap_or_default(),
                    text,
                    reasoning,
                    ..TurnOutcome::default()
                },
                Err(error) => TurnOutcome {
                    ok: false,
                    retryable: false,
                    message: format!("rkllm run failed: {error}"),
                    text,
                    reasoning,
                    ..TurnOutcome::default()
                },
            }
        });
        match tokio::time::timeout(Duration::from_secs(120), task).await {
            Ok(Ok(outcome)) => outcome,
            Ok(Err(join)) => TurnOutcome {
                ok: false,
                retryable: true,
                message: format!("rkllm task failed to join: {join}"),
                ..TurnOutcome::default()
            },
            Err(_) => {
                abort_model.abort();
                TurnOutcome {
                    ok: false,
                    retryable: true,
                    message: "rkllm inference timed out after 120s".to_owned(),
                    ..TurnOutcome::default()
                }
            }
        }
    }
}

enum ThinkPart {
    Reason(String),
    Text(String),
}

/// Splits a chunk on `<think>` / `</think>` tags, keeping the current
/// thinking state across chunks.
fn split_thinking(raw: &str, in_think: &mut bool) -> Vec<ThinkPart> {
    let mut parts = Vec::new();
    let mut rest = raw;
    while !rest.is_empty() {
        let open = rest.find("<think>");
        let close = rest.find("</think>");
        if *in_think {
            match close {
                Some(idx) => {
                    push_part(&mut parts, ThinkPart::Reason(rest[..idx].to_owned()));
                    *in_think = false;
                    rest = &rest[idx + "</think>".len()..];
                }
                None => {
                    push_part(&mut parts, ThinkPart::Reason(rest.to_owned()));
                    break;
                }
            }
        } else {
            match open {
                Some(idx) => {
                    if idx > 0 {
                        push_part(&mut parts, ThinkPart::Text(rest[..idx].to_owned()));
                    }
                    *in_think = true;
                    rest = &rest[idx + "<think>".len()..];
                }
                None => {
                    push_part(&mut parts, ThinkPart::Text(rest.to_owned()));
                    break;
                }
            }
        }
    }
    parts
}

fn push_part(parts: &mut Vec<ThinkPart>, part: ThinkPart) {
    match &part {
        ThinkPart::Reason(s) | ThinkPart::Text(s) if s.is_empty() => {}
        _ => parts.push(part),
    }
}

/// Builds `(system_prompt, prompt)` from an OpenAI chat body.
///
/// The RKLLM runtime applies the model's built-in chat template itself, so
/// only the raw latest user/tool message is passed — role prefixes or manual
/// history formatting confuse Qwen's tokenizer and can wedge the runtime.
fn build_prompt(body: &Value) -> (String, String) {
    let mut system = String::new();
    let Some(messages) = body.get("messages").and_then(Value::as_array) else {
        return (system, String::new());
    };
    for message in messages {
        let role = message.get("role").and_then(Value::as_str).unwrap_or("");
        if role == "system" {
            if let Some(content) = message.get("content").map(extract_text) {
                system = content;
            }
        }
    }
    // Latest user/tool message, raw text without any formatting.
    let prompt = messages
        .iter()
        .rev()
        .find(|message| {
            let role = message.get("role").and_then(Value::as_str).unwrap_or("");
            role == "user" || role == "tool"
        })
        .and_then(|message| message.get("content").map(extract_text))
        .unwrap_or_default();
    (system, prompt)
}

fn extract_text(value: &Value) -> String {
    match value {
        Value::String(text) => text.clone(),
        Value::Array(parts) => parts
            .iter()
            .filter_map(|part| part.get("text").and_then(Value::as_str))
            .collect::<Vec<_>>()
            .join(" "),
        _ => value.to_string(),
    }
}
