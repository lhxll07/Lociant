//! Peer-plane HTTP routes: tools/models a sibling node can use. The provider
//! decides what is visible — it applies its own `toolExposure` before
//! returning descriptors and enforces policy on every call.

use axum::extract::{Path, State};
use axum::Json;
use serde_json::{json, Value};

use crate::error::{Problem, RequireAuth, RequirePeerAuth};
use crate::models::collect_local_models;
use crate::state::AppState;

pub async fn list_peer_tools(
    State(state): State<AppState>,
    _: RequirePeerAuth,
) -> Json<Value> {
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    Json(json!({ "data": state.tools.visible(&exposure) }))
}

pub async fn call_peer_tool(
    State(state): State<AppState>,
    _: RequirePeerAuth,
    Path(tool_name): Path<String>,
    Json(body): Json<Value>,
) -> Result<Json<Value>, Problem> {
    let arguments = body.get("arguments").cloned().unwrap_or(Value::Null);
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    match state.tools.call_remote(&tool_name, arguments, &exposure) {
        Ok(result) => Ok(Json(json!({ "data": result }))),
        Err(error) => Err(Problem::bad_request(error.to_string(), "/api/v1/peer/tools")),
    }
}

pub async fn list_peer_models(
    State(state): State<AppState>,
    _: RequirePeerAuth,
) -> Json<Value> {
    Json(json!({ "models": collect_local_models(&state) }))
}

/// `/api/v1/nodes` — this node plus every discovered peer, for the UI.
pub async fn list_nodes(State(state): State<AppState>, _: RequireAuth) -> Json<Value> {
    let settings = state.settings_snapshot();
    let mut nodes = vec![json!({
        "id": settings.get("peerId").and_then(Value::as_str).unwrap_or("self"),
        "name": settings.get("peerName").and_then(Value::as_str).unwrap_or("本机"),
        "platform": std::env::consts::OS,
        "host": settings.get("host").and_then(Value::as_str).unwrap_or("127.0.0.1"),
        "port": state.port,
        "self": true,
        "online": true,
    })];
    if let Some(peers) = &state.peers {
        for node in peers.nodes() {
            nodes.push(json!({
                "id": node.id,
                "name": node.name,
                "platform": node.platform,
                "host": node.host.to_string(),
                "port": node.port,
                "self": false,
                "online": node.last_seen.elapsed() < std::time::Duration::from_secs(60),
            }));
        }
    }
    Json(json!({ "nodes": nodes }))
}
