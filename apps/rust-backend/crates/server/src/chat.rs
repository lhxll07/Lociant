//! OpenAI data plane: `/v1/chat/completions` runs the Rust agent loop against
//! the configured cloud backend, streaming the same SSE events the Flutter UI
//! already parses. Tools are executed through the shared `ToolRegistry`; with
//! no device adapter the loop degrades to a single cloud round.

use std::sync::Arc;

use axum::body::Body;
use axum::extract::State;
use axum::http::header::CONTENT_TYPE;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use bytes::Bytes;
use futures_util::StreamExt;
use lociant_agent::backend::{ChatBackend, CloudBackend};
use lociant_agent::{AgentConfig, LoopMessage};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use tokio::sync::mpsc;
use tokio_stream::wrappers::ReceiverStream;

use crate::control::valid_session_id;
use crate::device::IpcChatBackend;
use crate::error::{Problem, RequireChatAuth};
use crate::rkllm_backend::RkllmBackend;
use crate::state::AppState;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct UpstreamMessage {
    pub(crate) role: String,
    #[serde(default)]
    pub(crate) content: Value,
    #[serde(default)]
    pub(crate) tool_call_id: Option<String>,
    #[serde(default)]
    pub(crate) name: Option<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ChatRequest {
    pub(crate) model: String,
    pub(crate) messages: Vec<UpstreamMessage>,
    #[serde(default)]
    pub(crate) stream: bool,
    #[serde(default)]
    pub(crate) session_id: String,
    #[serde(default, rename = "max_tokens")]
    pub(crate) max_tokens: Option<u32>,
    #[serde(default)]
    pub(crate) tools: Option<Value>,
    #[serde(default, rename = "execute_tools")]
    pub(crate) execute_tools: bool,
    #[serde(default, rename = "stream_options")]
    pub(crate) stream_options: Option<Value>,
}

pub async fn chat_completions(
    State(state): State<AppState>,
    _: RequireChatAuth,
    Json(request): Json<ChatRequest>,
) -> Result<Response, Problem> {
    let settings = state.settings_snapshot();
    let cloud_enabled = settings
        .get("cloudEnabled")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let cloud_base = settings
        .get("cloudBaseUrl")
        .and_then(Value::as_str)
        .unwrap_or("")
        .trim_end_matches('/')
        .to_owned();
    let cloud_model = settings
        .get("cloudModel")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let cloud_api_key = settings
        .get("cloudApiKey")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let cloud_history_limit = settings
        .get("cloudHistoryLimit")
        .and_then(Value::as_u64)
        .map(|v| v as usize)
        .unwrap_or(256);
    let max_rounds = settings
        .get("agentMaxRounds")
        .and_then(Value::as_u64)
        .map(|v| {
            v.clamp(
                lociant_agent::ROUNDS_MIN as u64,
                lociant_agent::ROUNDS_MAX as u64,
            ) as u32
        })
        .unwrap_or(lociant_agent::ROUNDS_DEFAULT);
    let exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    let include_usage = request
        .stream_options
        .as_ref()
        .and_then(|o| o.get("include_usage"))
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let enable_thinking = settings
        .get("enableThinking")
        .and_then(Value::as_bool)
        .unwrap_or(false);

    let cloud_ready = cloud_enabled && !cloud_base.is_empty() && !cloud_model.is_empty();
    // Local models (MNN via IPC, or an RKLLM server configured with
    // `localModel: true`) skip tool definitions by default to save tokens.
    let mut local_model = settings
        .get("localModel")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let rkllm_model = settings
        .get("rkllmModelName")
        .and_then(Value::as_str)
        .unwrap_or("rkllm")
        .to_owned();
    let backend: Arc<dyn ChatBackend> = {
        // Node ids may contain a colon (manual peers are "host:port"), so
        // split from the right — model names rarely contain colons.
        if let Some((node_id, model_id)) = request
            .model
            .strip_prefix("peer:")
            .and_then(|rest| rest.rsplit_once(':'))
        {
            let Some(peers) = &state.peers else {
                return Err(Problem::bad_request(
                    format!("unknown peer node: {node_id}"),
                    "/v1/chat/completions",
                ));
            };
            let Some((base_url, peer_token)) = peers.peer_base_url(node_id) else {
                return Err(Problem::bad_request(
                    format!("peer node offline: {node_id}"),
                    "/v1/chat/completions",
                ));
            };
            Arc::new(crate::peers::PeerChatBackend {
                inner: CloudBackend {
                    client: state.http.clone(),
                    base_url,
                    api_key: peer_token,
                    model: model_id.to_owned(),
                },
                model_id: model_id.to_owned(),
            })
        } else {
            select_local_backend(
                &state,
                &request,
                &rkllm_model,
                cloud_ready,
                &cloud_model,
                &cloud_base,
                &cloud_api_key,
                enable_thinking,
                &mut local_model,
            )?
        }
    };

    let session_id = request.session_id.trim().to_owned();
    if !session_id.is_empty() {
        if !valid_session_id(&session_id) {
            return Err(Problem::not_found(
                format!("Session not found: {session_id}"),
                "/v1/chat/completions",
            ));
        }
        let exists = state
            .store
            .get_session(&session_id)
            .map_err(|e| Problem::internal(e.to_string()))?
            .is_some();
        if !exists {
            return Err(Problem::not_found(
                format!("Session not found: {session_id}"),
                "/v1/chat/completions",
            ));
        }
    }

    // The Flutter client sends only the newest user message; when a session is
    // explicit and history is short, prepend stored history like the Android
    // runtime does.
    let mut history: Vec<LoopMessage> = Vec::new();
    if !session_id.is_empty() && request.messages.len() <= 1 {
        let stored = state
            .store
            .list_messages(&session_id)
            .map_err(|e| Problem::internal(e.to_string()))?;
        let start = stored.len().saturating_sub(cloud_history_limit);
        for message in &stored[start..] {
            history.push(LoopMessage::user(json!(message.text)));
        }
    }
    history.extend(request.messages.iter().map(loop_message_from_request));

    if !session_id.is_empty() {
        for message in &request.messages {
            if message.role == "user" {
                let text = message_text(&message.content);
                if let Err(error) =
                    state
                        .store
                        .append_message(&session_id, "user", &text, json!({}))
                {
                    tracing::warn!("persist user message failed: {error}");
                }
            }
        }
    }

    let request_tools = request.tools.clone();
    let upstream_model = if request.model.is_empty() {
        cloud_model.clone()
    } else {
        request.model.clone()
    };

    if request.stream {
        let (tx, rx) = mpsc::channel::<Bytes>(128);
        let task_state = state.clone();
        let task_backend = backend.clone();
        let task_registry = state.tools.clone();
        let task_session = session_id.clone();
        tokio::spawn(async move {
            let config = AgentConfig {
                exposure: exposure.clone(),
                model: upstream_model.clone(),
                request_tools,
                execute_tools: request.execute_tools,
                local_model,
                enable_thinking,
                max_rounds,
                include_usage,
                max_tokens: request.max_tokens,
            };
            let result =
                lociant_agent::stream_agent(task_backend, task_registry, config, history, &tx)
                    .await;
            if !result.aborted
                && !task_session.is_empty()
                && (!result.text.is_empty() || !result.reasoning.is_empty())
            {
                let content = if result.reasoning.is_empty() {
                    json!({})
                } else {
                    json!({ "reasoning": result.reasoning })
                };
                if let Err(error) = task_state.store.append_message(
                    &task_session,
                    "assistant",
                    &result.text,
                    content,
                ) {
                    tracing::warn!("persist assistant message failed: {error}");
                }
            }
        });
        let body = Body::from_stream(ReceiverStream::new(rx).map(Ok::<_, std::io::Error>));
        return Ok(Response::builder()
            .status(StatusCode::OK)
            .header(CONTENT_TYPE, "text/event-stream")
            .header("Cache-Control", "no-cache")
            .body(body)
            .expect("static response builder"));
    }

    let result = lociant_agent::run_agent_complete(
        backend,
        state.tools.clone(),
        AgentConfig {
            exposure,
            model: upstream_model.clone(),
            request_tools,
            execute_tools: request.execute_tools,
            local_model,
            enable_thinking,
            max_rounds,
            include_usage,
            max_tokens: request.max_tokens,
        },
        history,
    )
    .await;
    if !session_id.is_empty() && (!result.text.is_empty() || !result.reasoning.is_empty()) {
        let content = if result.reasoning.is_empty() {
            json!({})
        } else {
            json!({ "reasoning": result.reasoning })
        };
        if let Err(error) =
            state
                .store
                .append_message(&session_id, "assistant", &result.text, content)
        {
            tracing::warn!("persist assistant message failed: {error}");
        }
    }
    Ok(Json(lociant_agent::complete_response(
        &upstream_model,
        &result,
        &session_id,
    ))
    .into_response())
}

fn select_local_backend(
    state: &AppState,
    request: &ChatRequest,
    rkllm_model: &str,
    cloud_ready: bool,
    cloud_model: &str,
    cloud_base: &str,
    cloud_api_key: &str,
    enable_thinking: bool,
    local_model: &mut bool,
) -> Result<Arc<dyn ChatBackend>, Problem> {
    if let Some(rkllm) = &state.rkllm {
        if request.model.is_empty() || request.model == rkllm_model {
            *local_model = true;
            return Ok(Arc::new(RkllmBackend {
                model: rkllm.clone(),
                enable_thinking,
            }));
        }
    }
    if cloud_ready && (request.model.is_empty() || request.model == cloud_model) {
        return Ok(Arc::new(CloudBackend {
            client: state.http.clone(),
            base_url: cloud_base.to_owned(),
            api_key: cloud_api_key.to_owned(),
            model: cloud_model.to_owned(),
        }));
    }
    if let Some(device) = &state.device {
        if device.has_model(&request.model) {
            *local_model = true;
            return Ok(Arc::new(IpcChatBackend {
                port: device.port,
                token: device.token.clone(),
            }));
        }
    }
    let detail = if cloud_ready {
        format!("unknown model: {}", request.model)
    } else {
        "no chat backend available: configure a cloud model or install a local model".into()
    };
    Err(Problem::bad_request(detail, "/v1/chat/completions"))
}

fn loop_message_from_request(message: &UpstreamMessage) -> LoopMessage {
    LoopMessage {
        role: message.role.clone(),
        content: message.content.clone(),
        reasoning: None,
        tool_calls: Vec::new(),
        tool_call_id: message.tool_call_id.clone(),
        name: message.name.clone(),
    }
}

/// Renders a user message's `content` (string or OpenAI parts array) into the
/// plain text persisted in the session.
fn message_text(content: &Value) -> String {
    match content {
        Value::String(text) => text.clone(),
        Value::Array(parts) => parts
            .iter()
            .filter_map(|part| part.get("text").and_then(Value::as_str))
            .collect::<Vec<_>>()
            .join("\n"),
        _ => String::new(),
    }
}
