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

    let mut runtime = lociant_core::RuntimeState {
        running: true,
        port: state.port,
        url: format!("http://127.0.0.1:{}", state.port),
        lan_url: state
            .lan_ip()
            .map(|ip| format!("http://{ip}:{}", state.port))
            .unwrap_or_default(),
        auth_token: if address.ip().is_loopback() {
            state.auth_token()
        } else {
            String::new()
        },
        model_id,
        ..Default::default()
    };
    let llama_status = state.llama.as_ref().map(|llama| llama.status());
    runtime.model_loaded =
        device_has_model || state.rkllm.is_some() || llama_status == Some("ready");
    runtime.tool_exposure = settings
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    runtime.auto_start = settings
        .get("autoStart")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    runtime.message = match llama_status {
        Some("restarting") => "edge runtime running; llama.cpp is recovering".to_owned(),
        Some("offline") => "edge runtime running; llama.cpp is unavailable".to_owned(),
        _ => "edge runtime running".to_owned(),
    };
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
) -> Result<Json<Value>, Problem> {
    let state_for_blocking = state.clone();
    tokio::task::spawn_blocking(move || state_for_blocking.merge_settings(&patch))
        .await
        .map_err(|error| Problem::internal(error.to_string()))?;
    Ok(Json(state.public_settings_snapshot()))
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

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;
    use std::io::{BufRead, BufReader, Write};
    use std::net::TcpListener;
    use std::sync::{mpsc, Arc, Mutex};
    use std::time::Duration;

    use crate::device::IpcDeviceAdapter;
    use lociant_store::Store;
    use lociant_tools::{NoopDevice, ToolRegistry};

    #[tokio::test(flavor = "current_thread")]
    async fn settings_update_keeps_executor_responsive_during_device_sync() {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let port = listener.local_addr().unwrap().port();
        let (started, observed) = tokio::sync::oneshot::channel();
        let (resume, released) = mpsc::channel();
        let device_thread = std::thread::spawn(move || {
            let (stream, _) = listener.accept().unwrap();
            stream
                .set_read_timeout(Some(Duration::from_secs(2)))
                .unwrap();
            let mut reader = BufReader::new(stream);
            let mut line = String::new();
            reader.read_line(&mut line).unwrap();
            let request: Value = serde_json::from_str(&line).unwrap();
            started.send(()).unwrap();
            let responsive = released.recv_timeout(Duration::from_secs(1)).is_ok();
            writeln!(reader.get_mut(), "{{\"ok\":true}}").unwrap();
            (responsive, request)
        });

        let data_dir = std::env::temp_dir().join(format!(
            "lociant-settings-test-{}-{port}",
            std::process::id()
        ));
        let state = AppState {
            store: Store::open(&data_dir).unwrap(),
            settings: Arc::new(Mutex::new(json!({
                "toolExposure": "action",
                "authToken": "old-token"
            }))),
            port: 11434,
            download_http: reqwest::Client::new(),
            tools: Arc::new(ToolRegistry::new(Box::new(NoopDevice))),
            device: Some(Arc::new(IpcDeviceAdapter {
                port,
                token: "device-token".to_owned(),
            })),
            catalog: Arc::new(Vec::new()),
            models_dir: data_dir.clone(),
            installs: Arc::new(Mutex::new(HashMap::new())),
            rkllm: None,
            llama: None,
            peers: None,
            tools_cache: Arc::new(Mutex::new(HashMap::new())),
        };

        let update = put_settings(
            State(state.clone()),
            RequireAuth,
            Json(json!({
                "toolExposure": "read",
                "authToken": "new-token"
            })),
        );
        let unrelated_async_work = async {
            observed.await.unwrap();
            let _ = resume.send(());
        };
        let (response, ()) = tokio::join!(update, unrelated_async_work);
        let (responsive, request) = device_thread.join().unwrap();
        let response = response.unwrap().0;
        drop(state);
        std::fs::remove_dir_all(data_dir).unwrap();

        assert!(responsive, "device settings IPC blocked the async executor");
        assert_eq!(request["method"], "settings.sync");
        assert_eq!(request["settings"]["toolExposure"], "read");
        assert_eq!(request["settings"]["authToken"], "new-token");
        assert_eq!(response["toolExposure"], "read");
        assert_eq!(response["authToken"], "");
    }
}
