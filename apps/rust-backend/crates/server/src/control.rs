use axum::extract::connect_info::ConnectInfo;
use axum::extract::{Path, State};
use axum::Json;
use lociant_tools::ToolError;
use serde_json::{json, Value};

use crate::error::{Problem, RequireAuth};
use crate::state::AppState;

pub async fn runtime(
    ConnectInfo(address): ConnectInfo<std::net::SocketAddr>,
    State(state): State<AppState>,
    _: RequireAuth,
) -> Result<Json<lociant_core::RuntimeState>, Problem> {
    let settings = state.settings_snapshot();
    let model_id = settings
        .get("modelId")
        .and_then(Value::as_str)
        .filter(|s| !s.is_empty())
        .map(str::to_owned)
        .unwrap_or_default();
    let device_has_model = if let Some(device) = &state.device {
        let device = device.clone();
        let model_id_for_block = model_id.clone();
        tokio::task::spawn_blocking(move || device.has_model(&model_id_for_block))
            .await
            .unwrap_or(false)
    } else {
        false
    };

    let mut runtime = lociant_core::RuntimeState::default();
    runtime.running = true;
    runtime.port = state.port;
    runtime.url = format!("http://127.0.0.1:{}", state.port);
    runtime.lan_url = state
        .lan_ip()
        .map(|ip| format!("http://{ip}:{}", state.port))
        .unwrap_or_default();
    runtime.auth_token = if address.ip().is_loopback() {
        state.auth_token()
    } else {
        String::new()
    };
    runtime.model_id = model_id;
    runtime.model_loaded = device_has_model || state.rkllm.is_some() || state.llama.is_some();
    runtime.tool_exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    runtime.auto_start = settings
        .get("autoStart")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    runtime.message = "edge runtime running".to_owned();
    runtime.device = json!({
        "os": std::env::consts::OS,
        "arch": std::env::consts::ARCH,
    });
    Ok(Json(runtime))
}

pub async fn get_settings(State(state): State<AppState>, _: RequireAuth) -> Json<Value> {
    Json(state.public_settings_snapshot())
}

pub async fn put_settings(
    State(state): State<AppState>,
    _: RequireAuth,
    Json(patch): Json<Value>,
) -> Json<Value> {
    state.merge_settings(&patch);
    Json(state.public_settings_snapshot())
}

pub async fn list_tools(State(state): State<AppState>, _: RequireAuth) -> Json<Value> {
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    if let Some(cached) = state.tools_cached(&exposure) {
        return Json(json!({ "data": cached }));
    }

    // Device adapters may perform local IPC or peer HTTP calls; keep blocking
    // work off the async executor.
    let registry = state.tools.clone();
    let exposure_for_block = exposure.clone();
    let tools = tokio::task::spawn_blocking(move || registry.visible(&exposure_for_block))
        .await
        .unwrap_or_default();
    state.set_tools_cache(&exposure, tools.clone());
    Json(json!({ "data": tools }))
}

pub async fn call_tool(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(tool_name): Path<String>,
    Json(body): Json<Value>,
) -> Result<Json<Value>, Problem> {
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    let arguments = body.get("arguments").cloned().unwrap_or(Value::Null);
    let instance = format!("/api/v1/tools/{tool_name}/calls");
    let registry = state.tools.clone();
    let exposure_for_block = exposure.clone();
    let tool_name_for_block = tool_name.clone();
    let result = tokio::task::spawn_blocking(move || {
        registry.call_remote(&tool_name_for_block, arguments, &exposure_for_block)
    })
    .await
    .unwrap_or_else(|error| Err(ToolError::Adapter(format!("tool task failed: {error}"))));

    match result {
        Ok(result) => Ok(Json(json!(result))),
        Err(ToolError::Unavailable(message)) => Err(Problem::not_found(message, &instance)),
        Err(ToolError::NotAllowed(message)) => Err(Problem::forbidden(message, &instance)),
        Err(ToolError::Adapter(message)) => Err(Problem::bad_request(message, &instance)),
    }
}
