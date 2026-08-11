//! Peer-plane HTTP routes: tools/models a sibling node can use. The provider
//! decides what is visible — it applies its own `toolExposure` before
//! returning descriptors and enforces policy on every call.

use axum::extract::{Path, State};
use axum::Json;
use serde_json::{json, Value};

use crate::error::{Problem, RequireAuth, RequireChatAuth, RequirePeerAuth};
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

/// `/api/v1/baby/state` — 眠安智护监控快照（需在配置中启用 babyCamera）。
pub async fn baby_state(State(state): State<AppState>, _: RequireChatAuth) -> Json<Value> {
    match &state.baby {
        Some(baby) => Json(baby.snapshot()),
        None => Json(json!({ "error": "baby monitor not enabled (set babyCamera in config)" })),
    }
}

/// `POST /api/v1/peers` — 手动添加节点（host/port/name），不依赖 mDNS。
pub async fn add_peer(
    State(state): State<AppState>,
    _: RequireAuth,
    Json(body): Json<Value>,
) -> Json<Value> {
    let Some(peers) = &state.peers else {
        return Json(json!({ "error": "peer networking not enabled (set peerToken in config)" }));
    };
    let host = body
        .get("host")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let port = body.get("port").and_then(Value::as_u64).unwrap_or(0) as u16;
    if host.is_empty() || port == 0 {
        return Json(json!({ "error": "host and port are required" }));
    }
    let name = body
        .get("name")
        .and_then(Value::as_str)
        .map(str::to_owned);
    peers.add_manual_peer(host, port, name);
    Json(json!({ "ok": true }))
}

/// `DELETE /api/v1/peers/{node_id}` — 移除一个节点。
pub async fn remove_peer(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(node_id): Path<String>,
) -> Json<Value> {
    if let Some(peers) = &state.peers {
        peers.remove_peer(&node_id);
        Json(json!({ "ok": true }))
    } else {
        Json(json!({ "error": "peer networking not enabled" }))
    }
}

/// `GET /api/v1/peers/{node_id}/baby/state` — 查看某节点的眠安智护监控。
pub async fn peer_baby_state(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(node_id): Path<String>,
) -> Json<Value> {
    let Some(peers) = &state.peers else {
        return Json(json!({ "error": "peer networking not enabled" }));
    };
    let Some((base_url, token)) = peers.peer_base_url(&node_id) else {
        return Json(json!({ "error": format!("peer node offline: {node_id}") }));
    };
    let base = base_url.trim_end_matches("/v1");
    let response = state
        .http
        .get(format!("{base}/api/v1/baby/state"))
        .bearer_auth(&token)
        .send()
        .await;
    match response {
        Ok(response) => match response.json::<Value>().await {
            Ok(body) => Json(body),
            Err(_) => Json(json!({ "error": "bad response from peer" })),
        },
        Err(error) => Json(json!({ "error": format!("peer request failed: {error}") })),
    }
}
