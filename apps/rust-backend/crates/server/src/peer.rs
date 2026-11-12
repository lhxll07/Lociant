//! Peer-plane HTTP routes: tools/models a sibling node can use. The provider
//! decides what is visible — it applies its own `toolExposure` before
//! returning descriptors and enforces policy on every call.

use axum::extract::{Path, State};
use axum::http::{header::HOST, HeaderMap, StatusCode};
use axum::Json;
use serde_json::{json, Value};

use crate::error::{Problem, RequireAuth, RequirePeerAuth};
use crate::models::collect_local_models;
use crate::state::AppState;

pub async fn list_peer_tools(
    State(state): State<AppState>,
    _: RequirePeerAuth,
) -> Result<Json<Value>, Problem> {
    let exposure = state
        .settings_snapshot()
        .get("toolExposure")
        .and_then(Value::as_str)
        .unwrap_or("action")
        .to_owned();
    // The peer plane serves this node's own tools only; aggregating peer
    // adapters here would make sibling nodes recurse into each other.
    // Local adapters can wait on device IPC, just like the control plane.
    let registry = state.tools.clone();
    let tools = tokio::task::spawn_blocking(move || registry.local_visible(&exposure))
        .await
        .map_err(|error| Problem::internal(error.to_string()))?;
    Ok(Json(json!({ "data": tools })))
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
    let registry = state.tools.clone();
    let result = tokio::task::spawn_blocking(move || {
        registry.call_remote_local(&tool_name, arguments, &exposure)
    })
    .await
    .map_err(|error| Problem::internal(error.to_string()))?;
    match result {
        Ok(result) => Ok(Json(json!({ "data": result }))),
        Err(error) => Err(Problem::bad_request(
            error.to_string(),
            "/api/v1/peer/tools",
        )),
    }
}

pub async fn list_peer_models(
    State(state): State<AppState>,
    _: RequirePeerAuth,
) -> Result<Json<Value>, Problem> {
    let models = tokio::task::spawn_blocking(move || collect_local_models(&state))
        .await
        .map_err(|error| Problem::internal(error.to_string()))?;
    Ok(Json(json!({ "models": models })))
}

/// `/api/v1/nodes` — this node plus every discovered peer, for the UI.
pub async fn list_nodes(
    State(state): State<AppState>,
    headers: HeaderMap,
    _: RequireAuth,
) -> Json<Value> {
    let settings = state.settings_snapshot();
    let request_host = headers.get(HOST).and_then(|value| value.to_str().ok());
    let self_host = local_display_host(&settings, request_host);
    let mut self_node = json!({
        "id": settings.get("peerId").and_then(Value::as_str).unwrap_or("self"),
        "name": settings.get("peerName").and_then(Value::as_str).unwrap_or("本机"),
        "platform": std::env::consts::OS,
        "host": self_host,
        "port": state.port,
        "self": true,
        "online": true,
    });
    if let Some(homepage) = homepage_for_node(settings.get("homepage"), &self_host, state.port) {
        self_node["homepage"] = homepage;
    }
    let mut nodes = vec![self_node];
    if let Some(peers) = &state.peers {
        for node in peers.nodes() {
            let mut peer = json!({
                "id": node.id,
                "name": node.name,
                "platform": node.platform,
                "host": node.host.to_string(),
                "port": node.port,
                "self": false,
                "online": node.last_seen.elapsed() < std::time::Duration::from_secs(60),
            });
            if let Some(homepage) =
                homepage_for_node(node.homepage.as_ref(), &node.host.to_string(), node.port)
            {
                peer["homepage"] = homepage;
            }
            nodes.push(peer);
        }
    }
    Json(json!({ "nodes": nodes }))
}

/// Converts the generic homepage setting advertised by a node into the small,
/// resolved shape consumed by clients. Lociant never fetches or interprets the
/// page; it only supplies a safe URL to open.
fn homepage_for_node(raw: Option<&Value>, host: &str, default_port: u16) -> Option<Value> {
    let raw = raw?;
    let (title, configured_url, path, port) = match raw {
        Value::String(url) => (None, Some(url.as_str()), None, default_port),
        Value::Object(object) => {
            let port = match object.get("port") {
                Some(value) => u16::try_from(value.as_u64()?).ok()?,
                None => default_port,
            };
            (
                object
                    .get("title")
                    .and_then(Value::as_str)
                    .filter(|value| !value.trim().is_empty()),
                object.get("url").and_then(Value::as_str),
                object.get("path").and_then(Value::as_str),
                port,
            )
        }
        _ => return None,
    };
    let url = if let Some(url) = configured_url.filter(|url| !url.is_empty()) {
        if is_http_url(url) {
            url.to_owned()
        } else if url.starts_with('/') {
            homepage_local_url(host, port, url)?
        } else {
            return None;
        }
    } else {
        homepage_local_url(host, port, path.unwrap_or("/"))?
    };
    Some(json!({
        "title": title.unwrap_or("Homepage"),
        "url": url,
    }))
}

fn homepage_local_url(host: &str, port: u16, path: &str) -> Option<String> {
    if port == 0 || !path.starts_with('/') || path.contains('\\') {
        return None;
    }
    if path.chars().any(char::is_control) {
        return None;
    }
    let host = if host.contains(':') && !host.starts_with('[') {
        format!("[{host}]")
    } else {
        host.to_owned()
    };
    let url = format!("http://{host}:{port}{path}");
    reqwest::Url::parse(&url).ok().map(|_| url)
}

fn is_http_url(value: &str) -> bool {
    let Ok(url) = reqwest::Url::parse(value) else {
        return false;
    };
    matches!(url.scheme(), "http" | "https") && url.host_str().is_some()
}

fn local_display_host(settings: &Value, request_host: Option<&str>) -> String {
    let configured = settings.get("host").and_then(Value::as_str).unwrap_or("");
    match configured.parse::<std::net::IpAddr>() {
        Ok(std::net::IpAddr::V4(address)) if address.is_unspecified() => request_host
            .and_then(request_host_name)
            .unwrap_or_else(|| "127.0.0.1".to_owned()),
        Ok(std::net::IpAddr::V6(address)) if address.is_unspecified() => request_host
            .and_then(request_host_name)
            .unwrap_or_else(|| "[::1]".to_owned()),
        Ok(_) => configured.to_owned(),
        Err(_) => request_host
            .and_then(request_host_name)
            .unwrap_or_else(|| "127.0.0.1".to_owned()),
    }
}

fn request_host_name(value: &str) -> Option<String> {
    let value = value.trim();
    if value.is_empty() {
        return None;
    }
    if value.starts_with('[') {
        let end = value.find(']')?;
        return Some(value[..=end].to_owned());
    }
    if let Some((host, port)) = value.rsplit_once(':') {
        if port.parse::<u16>().is_ok() {
            return (!host.is_empty()).then(|| host.to_owned());
        }
    }
    Some(value.to_owned())
}

/// `POST /api/v1/peers` — 手动添加节点（host/port/name），不依赖 mDNS。
pub async fn add_peer(
    State(state): State<AppState>,
    _: RequireAuth,
    Json(body): Json<Value>,
) -> (StatusCode, Json<Value>) {
    let Some(peers) = &state.peers else {
        return (
            StatusCode::SERVICE_UNAVAILABLE,
            Json(json!({ "error": "peer networking not enabled (set peerToken in config)" })),
        );
    };
    let host = body
        .get("host")
        .and_then(Value::as_str)
        .unwrap_or("")
        .to_owned();
    let Some(port) = body
        .get("port")
        .and_then(Value::as_u64)
        .and_then(valid_port)
    else {
        return (
            StatusCode::BAD_REQUEST,
            Json(json!({ "error": "host is required and port must be between 1 and 65535" })),
        );
    };
    if host.is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(json!({ "error": "host is required" })),
        );
    }
    let name = body.get("name").and_then(Value::as_str).map(str::to_owned);
    if let Err(error) = peers.add_manual_peer(host, port, name) {
        return (StatusCode::BAD_REQUEST, Json(json!({ "error": error })));
    }
    persist_manual_peers(&state);
    (StatusCode::OK, Json(json!({ "ok": true })))
}

fn valid_port(value: u64) -> Option<u16> {
    u16::try_from(value).ok().filter(|port| *port != 0)
}

/// `DELETE /api/v1/peers/{node_id}` — 移除一个节点。
pub async fn remove_peer(
    State(state): State<AppState>,
    _: RequireAuth,
    Path(node_id): Path<String>,
) -> Json<Value> {
    if let Some(peers) = &state.peers {
        peers.remove_peer(&node_id);
        persist_manual_peers(&state);
        Json(json!({ "ok": true }))
    } else {
        Json(json!({ "error": "peer networking not enabled" }))
    }
}

/// Persists manually added peers into settings so they survive restarts.
fn persist_manual_peers(state: &AppState) {
    let Some(peers) = &state.peers else {
        return;
    };
    let list: Vec<Value> = peers
        .nodes()
        .iter()
        .filter(|node| node.platform == "manual")
        .map(|node| {
            json!({
                "host": node.host.to_string(),
                "port": node.port,
                "name": node.name,
            })
        })
        .collect();
    state.merge_settings(&json!({ "manualPeers": list }));
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;
    use std::io::{BufRead, BufReader, Write};
    use std::net::TcpListener;
    use std::sync::{mpsc, Arc, Mutex};
    use std::time::Duration;

    use axum::response::IntoResponse;
    use lociant_store::Store;
    use lociant_tools::ToolRegistry;

    use crate::device::IpcDeviceAdapter;

    async fn assert_peer_device_request_yields(method: &'static str) {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let port = listener.local_addr().unwrap().port();
        let (started, observed) = tokio::sync::oneshot::channel();
        let (resume, released) = mpsc::channel();
        let device_thread = std::thread::spawn(move || {
            let mut started = Some(started);
            let mut responsive = false;
            let requests = if method == "tools.call" { 2 } else { 1 };
            for _ in 0..requests {
                let (stream, _) = listener.accept().unwrap();
                stream
                    .set_read_timeout(Some(Duration::from_secs(5)))
                    .unwrap();
                let mut reader = BufReader::new(stream);
                let mut line = String::new();
                reader.read_line(&mut line).unwrap();
                let request: Value = serde_json::from_str(&line).unwrap();
                if request["method"] == method {
                    started.take().unwrap().send(()).unwrap();
                    // The async task below must run while the device waits.
                    // A bounded wait makes the old blocking handler fail
                    // without deadlocking the test runtime.
                    responsive = released.recv_timeout(Duration::from_secs(2)).is_ok();
                }
                let response = match request["method"].as_str().unwrap() {
                    "tools.list" => json!({
                        "ok": true,
                        "tools": [{
                            "function": { "name": "runtime_status" },
                            "x_policy": { "remoteAllowed": true }
                        }]
                    }),
                    "tools.call" => json!({
                        "ok": true,
                        "result": { "content": "device ready", "arguments": request["arguments"] }
                    }),
                    "models.list" => json!({
                        "ok": true,
                        "models": [{ "id": "device-model", "runtime": "llama" }]
                    }),
                    unexpected => panic!("unexpected IPC method: {unexpected}"),
                };
                writeln!(reader.get_mut(), "{response}").unwrap();
            }
            responsive
        });

        let data_dir =
            std::env::temp_dir().join(format!("lociant-peer-test-{}-{port}", std::process::id()));
        let device = Arc::new(IpcDeviceAdapter {
            port,
            token: String::new(),
        });
        let state = AppState {
            store: Store::open(&data_dir).unwrap(),
            settings: Arc::new(Mutex::new(json!({ "toolExposure": "read" }))),
            port: 11434,
            download_http: reqwest::Client::new(),
            tools: Arc::new(ToolRegistry::new(Box::new(IpcDeviceAdapter {
                port,
                token: String::new(),
            }))),
            device: Some(device),
            catalog: Arc::new(Vec::new()),
            models_dir: data_dir.clone(),
            installs: Arc::new(Mutex::new(HashMap::new())),
            rkllm: None,
            llama: None,
            peers: None,
            tools_cache: Arc::new(Mutex::new(HashMap::new())),
        };
        let request = async {
            match method {
                "tools.list" => list_peer_tools(State(state.clone()), RequirePeerAuth)
                    .await
                    .into_response(),
                "tools.call" => call_peer_tool(
                    State(state.clone()),
                    RequirePeerAuth,
                    Path("runtime_status".to_owned()),
                    Json(json!({ "arguments": { "value": 7 } })),
                )
                .await
                .into_response(),
                "models.list" => list_peer_models(State(state.clone()), RequirePeerAuth)
                    .await
                    .into_response(),
                _ => unreachable!(),
            }
        };
        let async_progress = async {
            observed.await.unwrap();
            let _ = resume.send(());
        };
        let (response, ()) = tokio::join!(request, async_progress);
        let responsive = device_thread.join().unwrap();
        drop(state);
        std::fs::remove_dir_all(data_dir).unwrap();

        assert!(responsive, "{method} blocked unrelated async work");
        assert_eq!(response.status(), StatusCode::OK);
        let bytes = axum::body::to_bytes(response.into_body(), 1024 * 1024)
            .await
            .unwrap();
        let body: Value = serde_json::from_slice(&bytes).unwrap();
        match method {
            "tools.list" => assert_eq!(body["data"][0]["name"], "runtime_status"),
            "tools.call" => {
                assert_eq!(body["data"]["ok"], true);
                assert_eq!(body["data"]["structured"]["arguments"]["value"], 7);
            }
            "models.list" => assert_eq!(body["models"][0]["id"], "device-model"),
            _ => unreachable!(),
        }
    }

    #[tokio::test(flavor = "current_thread")]
    async fn peer_tool_listing_yields_while_device_waits() {
        assert_peer_device_request_yields("tools.list").await;
    }

    #[tokio::test(flavor = "current_thread")]
    async fn peer_tool_call_yields_while_device_waits() {
        assert_peer_device_request_yields("tools.call").await;
    }

    #[tokio::test(flavor = "current_thread")]
    async fn peer_model_listing_yields_while_device_waits() {
        assert_peer_device_request_yields("models.list").await;
    }

    #[test]
    fn request_host_preserves_ipv6_brackets() {
        for (header, expected) in [
            ("[::1]", "[::1]"),
            ("[::1]:11434", "[::1]"),
            ("[2001:db8::2]:11434", "[2001:db8::2]"),
        ] {
            assert_eq!(
                request_host_name(header).as_deref(),
                Some(expected),
                "Host: {header}"
            );
        }
    }

    #[test]
    fn incomplete_ipv6_request_host_uses_loopback_fallback() {
        assert_eq!(request_host_name("[::1:11434"), None);
        assert_eq!(
            local_display_host(&json!({ "host": "::" }), Some("[::1:11434")),
            "[::1]"
        );
    }

    #[test]
    fn resolves_homepage_using_ipv6_request_host() {
        let settings = json!({ "host": "::" });
        let host = local_display_host(&settings, Some("[2001:db8::2]:11434"));
        assert_eq!(
            homepage_for_node(
                Some(&json!({ "port": 11436, "path": "/dashboard" })),
                &host,
                11434,
            ),
            Some(json!({
                "title": "Homepage",
                "url": "http://[2001:db8::2]:11436/dashboard"
            }))
        );
    }

    #[test]
    fn request_host_strips_ipv4_and_hostname_ports() {
        for (header, expected) in [
            ("192.168.10.103:11434", "192.168.10.103"),
            ("board.local:11434", "board.local"),
            ("board.local", "board.local"),
        ] {
            assert_eq!(request_host_name(header).as_deref(), Some(expected));
        }
    }

    #[test]
    fn resolves_generic_homepage_against_discovered_host() {
        let raw = json!({
            "title": "Monitor",
            "port": 11436,
            "path": "/"
        });
        assert_eq!(
            homepage_for_node(Some(&raw), "192.168.10.103", 11434),
            Some(json!({
                "title": "Monitor",
                "url": "http://192.168.10.103:11436/"
            }))
        );
    }

    #[test]
    fn preserves_valid_absolute_homepage_url() {
        let raw = json!({
            "title": "Dashboard",
            "url": "https://example.test/monitor"
        });
        assert_eq!(
            homepage_for_node(Some(&raw), "192.168.10.103", 11434),
            Some(json!({
                "title": "Dashboard",
                "url": "https://example.test/monitor"
            }))
        );
    }

    #[test]
    fn rejects_unsupported_or_malformed_homepage() {
        assert!(
            homepage_for_node(Some(&json!("javascript:alert(1)")), "127.0.0.1", 11434).is_none()
        );
        assert!(homepage_for_node(
            Some(&json!({ "port": 11436, "path": "monitor" })),
            "127.0.0.1",
            11434,
        )
        .is_none());
    }
}
