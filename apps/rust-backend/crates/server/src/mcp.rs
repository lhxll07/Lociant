//! Minimal MCP Streamable-HTTP endpoint.
//!
//! Implements the stateless subset MCP clients actually use against a local
//! tool server: `initialize`, `ping`, `tools/list`, `tools/call` and
//! notifications (202 no-body). Responses are JSON (no SSE), which the
//! Streamable-HTTP spec permits; tools come from the shared `ToolRegistry`
//! and remote calls enforce `remote_allowed` like the control API.

use axum::extract::State;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use bytes::Bytes;
use lociant_tools::ToolError;
use serde_json::{json, Value};

use crate::error::RequireAuth;
use crate::state::AppState;

const DEFAULT_PROTOCOL_VERSION: &str = "2025-03-26";

pub async fn handle(State(state): State<AppState>, _: RequireAuth, body: Bytes) -> Response {
    let request: Value = match serde_json::from_slice(&body) {
        Ok(request) => request,
        Err(_) => return json_rpc_error(Value::Null, -32700, "Parse error").into_response(),
    };

    let id = request.get("id").cloned().unwrap_or(Value::Null);
    // Notifications carry no id; acknowledge with 202 and no body.
    if id.is_null() {
        return StatusCode::ACCEPTED.into_response();
    }

    let method = request.get("method").and_then(Value::as_str).unwrap_or("");
    let outcome = match method {
        "initialize" => Ok(initialize(&request)),
        "ping" => Ok(json!({})),
        "tools/list" => tools_list(&state).await,
        "tools/call" => tools_call(&state, &request).await,
        _ => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            -32601,
            "Method not found".to_owned(),
        )),
    };

    match outcome {
        Ok(result) => json_rpc_result(id, result).into_response(),
        Err((_, code, message)) => json_rpc_error(id, code, &message).into_response(),
    }
}

fn initialize(request: &Value) -> Value {
    let protocol_version = request
        .pointer("/params/protocolVersion")
        .and_then(Value::as_str)
        .unwrap_or(DEFAULT_PROTOCOL_VERSION);
    json!({
        "protocolVersion": protocol_version,
        "capabilities": { "tools": { "listChanged": false } },
        "serverInfo": {
            "name": "lociant",
            "version": env!("CARGO_PKG_VERSION"),
        },
        "instructions": "Use Lociant tools for device sensing and actions.",
    })
}

async fn tools_list(state: &AppState) -> Result<Value, (StatusCode, i64, String)> {
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    let registry = state.tools.clone();
    let exposure_for_block = exposure.clone();
    let tools = tokio::task::spawn_blocking(move || registry.visible(&exposure_for_block))
        .await
        .map_err(|error| {
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                -32603,
                format!("tools list failed: {error}"),
            )
        })?;
    let tools = tools
        .into_iter()
        .map(|tool| {
            json!({
                "name": tool.name,
                "description": tool.description,
                "inputSchema": if tool.arguments.is_null() {
                    json!({ "type": "object" })
                } else {
                    tool.arguments
                },
            })
        })
        .collect::<Vec<_>>();
    Ok(json!({ "tools": tools }))
}

async fn tools_call(state: &AppState, request: &Value) -> Result<Value, (StatusCode, i64, String)> {
    let name = request
        .pointer("/params/name")
        .and_then(Value::as_str)
        .unwrap_or("");
    if name.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            -32602,
            "Missing tool name".to_owned(),
        ));
    }
    let arguments = request
        .pointer("/params/arguments")
        .cloned()
        .unwrap_or_else(|| json!({}));
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    let registry = state.tools.clone();
    let name_for_block = name.to_owned();
    let exposure_for_block = exposure.clone();
    let result = tokio::task::spawn_blocking(move || {
        registry.call_remote(&name_for_block, arguments, &exposure_for_block)
    })
    .await
    .unwrap_or_else(|error| Err(ToolError::Adapter(format!("tool task failed: {error}"))));

    match result {
        Ok(result) => {
            let text = serde_json::to_string(&result).unwrap_or_else(|_| "{}".into());
            Ok(json!({
                "content": [{ "type": "text", "text": text }],
                "isError": !result.ok,
                "structuredContent": if result.structured.is_null() {
                    Value::Null
                } else {
                    result.structured
                },
            }))
        }
        Err(ToolError::Unavailable(message)) => Err((
            StatusCode::NOT_FOUND,
            -32602,
            format!("Tool not found: {message}"),
        )),
        Err(ToolError::NotAllowed(message)) => Err((StatusCode::FORBIDDEN, -32603, message)),
        // Execution failure is domain output, not a protocol error.
        Err(ToolError::Adapter(message)) => Ok(json!({
            "content": [{ "type": "text", "text": message }],
            "isError": true,
        })),
    }
}

fn json_rpc_result(id: Value, result: Value) -> Json<Value> {
    Json(json!({ "jsonrpc": "2.0", "id": id, "result": result }))
}

fn json_rpc_error(id: Value, code: i64, message: &str) -> Json<Value> {
    Json(json!({
        "jsonrpc": "2.0",
        "id": id,
        "error": { "code": code, "message": message },
    }))
}
