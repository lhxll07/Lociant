use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use lociant_tools::ToolError;
use serde_json::{json, Value};

use crate::error::{Problem, RequireAuth};
use crate::state::AppState;

pub async fn runtime(
    State(state): State<AppState>,
    _: RequireAuth,
) -> Result<Json<lociant_core::RuntimeState>, Problem> {
    let settings = state.settings_snapshot();
    let cloud_enabled = settings
        .get("cloudEnabled")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let cloud_model = settings
        .get("cloudModel")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let model_id = settings
        .get("modelId")
        .and_then(Value::as_str)
        .filter(|s| !s.is_empty())
        .unwrap_or(&cloud_model)
        .to_owned();

    let mut rt = lociant_core::RuntimeState::default();
    rt.running = true;
    rt.port = state.port;
    rt.url = format!("http://127.0.0.1:{}", state.port);
    rt.lan_url = state
        .lan_ip()
        .map(|ip| format!("http://{ip}:{}", state.port))
        .unwrap_or_default();
    rt.auth_token = state.auth_token();
    rt.model_id = model_id;
    rt.model_loaded = cloud_enabled && !cloud_model.is_empty();
    rt.inference_backend = settings
        .get("inferenceBackend")
        .and_then(Value::as_str)
        .unwrap_or("model")
        .to_owned();
    rt.cloud_enabled = cloud_enabled;
    rt.cloud_model = cloud_model;
    rt.cloud_base_url = settings
        .get("cloudBaseUrl")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    rt.cloud_api_key = settings
        .get("cloudApiKey")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    rt.cloud_max_output_tokens = settings
        .get("cloudMaxOutputTokens")
        .and_then(Value::as_u64)
        .map(|v| v as u32)
        .unwrap_or(rt.cloud_max_output_tokens);
    rt.tool_exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    rt.auto_start = settings
        .get("autoStart")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    rt.current_session_id = settings
        .get("currentSessionId")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    rt.message = "runtime running".to_owned();
    rt.device = json!({ "os": std::env::consts::OS, "arch": std::env::consts::ARCH });
    rt.sessions = state
        .store
        .list_sessions()
        .map_err(|e| Problem::internal(e.to_string()))?;
    Ok(Json(rt))
}

pub async fn get_settings(State(state): State<AppState>, _: RequireAuth) -> Json<Value> {
    Json(state.settings_snapshot())
}

pub async fn put_settings(
    State(state): State<AppState>,
    _: RequireAuth,
    Json(patch): Json<Value>,
) -> Json<Value> {
    Json(state.merge_settings(&patch))
}

pub async fn list_sessions(
    State(state): State<AppState>,
    _: RequireAuth,
) -> Result<Json<Value>, Problem> {
    let sessions = state
        .store
        .list_sessions()
        .map_err(|e| Problem::internal(e.to_string()))?;
    Ok(Json(json!({ "sessions": sessions })))
}

pub async fn create_session(
    State(state): State<AppState>,
    _: RequireAuth,
    body: Option<Json<Value>>,
) -> Result<Response, Problem> {
    let model_id = body
        .as_ref()
        .and_then(|b| b.0.get("modelId"))
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let id = state
        .store
        .create_session(&model_id)
        .map_err(|e| Problem::internal(e.to_string()))?;
    state.merge_settings(&json!({ "currentSessionId": id }));
    let details = session_details_json(&state, &id)?;
    Ok((StatusCode::CREATED, Json(details)).into_response())
}

pub async fn get_session(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(session_id): Path<String>,
) -> Result<Json<Value>, Problem> {
    Ok(Json(session_details_json(&state, &session_id)?))
}

pub async fn delete_session(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(session_id): Path<String>,
) -> Result<StatusCode, Problem> {
    if !valid_session_id(&session_id) {
        return Err(Problem::not_found(
            format!("Session not found: {session_id}"),
            &format!("/api/v1/sessions/{session_id}"),
        ));
    }
    let deleted = state
        .store
        .delete_session(&session_id)
        .map_err(|e| Problem::internal(e.to_string()))?;
    if !deleted {
        return Err(Problem::not_found(
            format!("Session not found: {session_id}"),
            &format!("/api/v1/sessions/{session_id}"),
        ));
    }
    let settings = state.settings_snapshot();
    if settings.get("currentSessionId").and_then(Value::as_str) == Some(session_id.as_str()) {
        state.merge_settings(&json!({ "currentSessionId": "" }));
    }
    Ok(StatusCode::NO_CONTENT)
}

pub async fn list_tools(State(state): State<AppState>, _: RequireAuth) -> Json<Value> {
    let settings = state.settings_snapshot();
    let exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    if let Some(cached) = state.tools_cached(&exposure) {
        return Json(json!({ "data": cached }));
    }
    let tools = state.tools.visible(&exposure);
    state.set_tools_cache(&exposure, tools.clone());
    Json(json!({ "data": tools }))
}

pub async fn call_tool(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(tool_name): Path<String>,
    Json(body): Json<Value>,
) -> Result<Json<Value>, Problem> {
    let settings = state.settings_snapshot();
    let exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    let arguments = body.get("arguments").cloned().unwrap_or(Value::Null);
    let instance = format!("/api/v1/tools/{tool_name}/calls");
    match state.tools.call_remote(&tool_name, arguments, &exposure) {
        Ok(result) => Ok(Json(json!(result))),
        Err(ToolError::Unavailable(message)) => Err(Problem::not_found(message, &instance)),
        Err(ToolError::NotAllowed(message)) => Err(Problem::forbidden(message, &instance)),
        Err(ToolError::Adapter(message)) => Err(Problem::bad_request(message, &instance)),
    }
}

// Problem is the uniform error type returned by every handler; the size lint
// is not actionable here without splitting the error hierarchy.
#[allow(clippy::result_large_err)]
fn session_details_json(state: &AppState, session_id: &str) -> Result<Value, Problem> {
    if !valid_session_id(session_id) {
        return Err(Problem::not_found(
            format!("Session not found: {session_id}"),
            &format!("/api/v1/sessions/{session_id}"),
        ));
    }
    let session = state
        .store
        .get_session(session_id)
        .map_err(|e| Problem::internal(e.to_string()))?
        .ok_or_else(|| {
            Problem::not_found(
                format!("Session not found: {session_id}"),
                &format!("/api/v1/sessions/{session_id}"),
            )
        })?;
    let messages = state
        .store
        .list_messages(session_id)
        .map_err(|e| Problem::internal(e.to_string()))?;
    Ok(json!({
        "id": session.id,
        "title": session.title,
        "modelId": session.model_id,
        "metadata": {},
        "updatedAt": session.updated_at,
        "messages": messages
            .iter()
            .map(|m| json!({
                "id": m.id,
                "role": m.role,
                "text": m.text,
                "contentJson": m.content_json,
            }))
            .collect::<Vec<_>>(),
    }))
}

pub fn valid_session_id(id: &str) -> bool {
    !id.is_empty()
        && id.len() <= 96
        && id
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || matches!(c, '.' | '_' | '-'))
}
