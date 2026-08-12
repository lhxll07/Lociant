mod baby;
mod catalog;
mod chat;
mod control;
mod device;
mod error;
mod init;
mod mcp;
mod models;
mod peer;
mod peers;
mod rkllm_backend;
mod state;

use std::collections::HashMap;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use axum::routing::{delete, get, post};
use axum::Router;
use device::IpcDeviceAdapter;
use lociant_store::Store;
use lociant_tools::{NoopDevice, ToolRegistry};
use serde_json::Value;

use crate::state::AppState;

const DEFAULT_PORT: u16 = 11434;

fn data_dir() -> PathBuf {
    if let Ok(dir) = std::env::var("LOCIANT_DATA_DIR") {
        return PathBuf::from(dir);
    }
    let base = std::env::var("XDG_DATA_HOME")
        .map(PathBuf::from)
        .unwrap_or_else(|_| {
            std::env::var("HOME")
                .map(|home| PathBuf::from(home).join(".local").join("share"))
                .unwrap_or_else(|_| PathBuf::from("."))
        });
    base.join("lociant")
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    if std::env::args().any(|arg| arg == "--init") {
        return init::run();
    }
    if std::env::args().any(|arg| arg == "--rkllm-test") {
        let prompt = std::env::args()
            .next_back()
            .unwrap_or_else(|| "你好".to_owned());
        let config_path = std::env::var("LOCIANT_CONFIG")
            .unwrap_or_else(|_| "/etc/lociant/config.json".to_owned());
        let config = std::fs::read_to_string(&config_path)
            .ok()
            .and_then(|raw| serde_json::from_str::<Value>(&raw).ok());
        let model_path = config
            .as_ref()
            .and_then(|config| config.get("rkllmModelPath"))
            .and_then(Value::as_str)
            .unwrap_or("/home/lhx/qwen3.5-0.8b.rkllm")
            .to_owned();
        let lib_path = config
            .as_ref()
            .and_then(|config| config.get("rkllmLibPath"))
            .and_then(Value::as_str)
            .map(str::to_owned);
        let model = lociant_rkllm::Rkllm::load(&model_path, lib_path.as_deref())?;
        let started = std::time::Instant::now();
        let out = lociant_rkllm::run_collect(&model, &prompt, "user", false, Some(200))?;
        println!("elapsed={:?}", started.elapsed());
        println!("OUT: {out}");
        return Ok(());
    }

    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()),
        )
        .init();

    let store = Store::open(&data_dir())?;
    let settings = store
        .get_json("settings")?
        .unwrap_or_else(|| Value::Object(Default::default()));
    // Headless bootstrap: a config file (`LOCIANT_CONFIG`) overrides stored
    // settings on every start, so a board can be configured without a UI.
    let settings = load_headless_config(settings);
    let port = settings
        .get("port")
        .and_then(Value::as_u64)
        .map(|v| v as u16)
        .or_else(|| {
            std::env::var("LOCIANT_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
        })
        .unwrap_or(DEFAULT_PORT);
    let models_dir = std::env::var("LOCIANT_MODELS_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|_| data_dir().join("models"));
    std::fs::create_dir_all(&models_dir)?;
    let host = settings
        .get("host")
        .and_then(Value::as_str)
        .and_then(|value| value.parse::<IpAddr>().ok())
        .or_else(|| {
            std::env::var("LOCIANT_HOST")
                .ok()
                .and_then(|value| value.parse::<IpAddr>().ok())
        })
        .unwrap_or(IpAddr::V4(Ipv4Addr::LOCALHOST));
    let rkllm = match settings.get("rkllmModelPath").and_then(Value::as_str) {
        Some(path) => {
            let lib_path = settings
                .get("rkllmLibPath")
                .and_then(Value::as_str)
                .map(str::to_owned);
            match lociant_rkllm::Rkllm::load(path, lib_path.as_deref()) {
                Ok(model) => {
                    tracing::info!("RKLLM loaded: {path}");
                    Some(Arc::new(model))
                }
                Err(error) => {
                    tracing::error!("RKLLM init failed ({path}): {error:#}");
                    None
                }
            }
        }
        None => None,
    };
    let device: Option<Arc<IpcDeviceAdapter>> = match (
        std::env::var(IpcDeviceAdapter::TOKEN_ENV),
        std::env::var(IpcDeviceAdapter::PORT_ENV),
    ) {
        (Ok(token), Ok(port)) if port.parse::<u16>().map(|p| p != 0).unwrap_or(false) => {
            tracing::info!("device adapter enabled (ipc port {port})");
            Some(Arc::new(IpcDeviceAdapter {
                port: port.parse().unwrap_or(0),
                token,
            }))
        }
        _ => None,
    };
    let tools: Arc<ToolRegistry> = match &device {
        Some(adapter) => Arc::new(ToolRegistry::new(Box::new(IpcDeviceAdapter {
            port: adapter.port,
            token: adapter.token.clone(),
        }))),
        None => Arc::new(ToolRegistry::new(Box::new(NoopDevice))),
    };

    // Peer networking is always enabled: without a peer token the plane is
    // open (like the control plane), so trusted LAN devices interconnect
    // out of the box. Setting `peerToken` on every node adds a shared secret.
    let peers = {
        let self_id = settings
            .get("peerId")
            .and_then(Value::as_str)
            .map(str::to_owned)
            .unwrap_or_else(|| {
                // A stable, unique peer id matters: discovery drops packets
                // whose id equals our own, so a shared default would make
                // every node ignore every other node. Prefer the host name
                // (or /etc/hostname on Android) plus the platform.
                let host = std::env::var("HOSTNAME")
                    .or_else(|_| std::env::var("HOST"))
                    .unwrap_or_default();
                let host = if host.is_empty() || host == "localhost" {
                    std::fs::read_to_string("/etc/hostname")
                        .ok()
                        .map(|s| s.trim().to_owned())
                        .filter(|s| !s.is_empty() && s != "localhost")
                        .unwrap_or_else(|| "lociant-node".to_owned())
                } else {
                    host
                };
                format!("{host}-{}", std::env::consts::OS)
            });
        let self_name = settings
            .get("peerName")
            .and_then(Value::as_str)
            .map(str::to_owned)
            .unwrap_or_else(|| self_id.clone());
        let peer_token = settings
            .get("peerToken")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned();
        Some(Arc::new(peers::PeerManager::new(
            tools.clone(),
            self_id,
            self_name,
            peer_token,
            port,
        )))
    };
    let peers_for_start = peers.clone();
    let peer_discovery = peer_discovery_enabled(&settings);
    let manual_peers = settings
        .get("manualPeers")
        .cloned()
        .unwrap_or(Value::Array(Vec::new()));

    let baby = settings
        .get("babyCamera")
        .and_then(Value::as_str)
        .filter(|device| !device.is_empty())
        .and_then(|device| {
            let mic = settings
                .get("babyMic")
                .and_then(Value::as_str)
                .filter(|s| !s.is_empty())
                .unwrap_or("default")
                .to_owned();
            crate::baby::start(device, &mic)
        });

    let state = AppState {
        store,
        settings: Arc::new(Mutex::new(settings)),
        port,
        http: reqwest::Client::builder()
            .connect_timeout(std::time::Duration::from_secs(15))
            .timeout(std::time::Duration::from_secs(15))
            .build()?,
        download_http: reqwest::Client::builder()
            .connect_timeout(std::time::Duration::from_secs(15))
            .timeout(std::time::Duration::from_secs(600))
            .build()?,
        tools,
        device,
        catalog: Arc::new(catalog::load()),
        models_dir,
        installs: Arc::new(Mutex::new(HashMap::new())),
        rkllm,
        peers,
        baby,
        baby_cache: Arc::new(Mutex::new(HashMap::new())),
        tools_cache: Arc::new(Mutex::new(HashMap::new())),
    };

    if let Some(peers) = peers_for_start {
        // Restore manually added peers persisted in settings.
        if let Some(list) = manual_peers.as_array() {
            for item in list {
                let Some(host) = item.get("host").and_then(Value::as_str) else {
                    continue;
                };
                let Some(port) = item.get("port").and_then(Value::as_u64) else {
                    continue;
                };
                let name = item.get("name").and_then(Value::as_str).map(str::to_owned);
                if let Err(error) = peers.add_manual_peer(host.to_owned(), port as u16, name) {
                    tracing::warn!(%error, "ignoring invalid persisted manual peer");
                }
            }
        }
        peers.start(peer_discovery);
    }

    let app = Router::new()
        .route("/health", get(health))
        .route("/api/v1/runtime", get(control::runtime))
        .route(
            "/api/v1/settings",
            get(control::get_settings).put(control::put_settings),
        )
        .route(
            "/api/v1/sessions",
            get(control::list_sessions).post(control::create_session),
        )
        .route(
            "/api/v1/sessions/{session_id}",
            get(control::get_session).delete(control::delete_session),
        )
        .route("/api/v1/models", get(models::list_models))
        .route("/api/v1/models/{model_id}", delete(models::delete_model))
        .route("/api/v1/catalog/models", get(models::catalog_models))
        .route("/api/v1/model-installations", post(models::start_install))
        .route(
            "/api/v1/model-installations/{job_id}",
            get(models::install_progress),
        )
        .route("/api/v1/tools", get(control::list_tools))
        .route("/api/v1/tools/{tool_name}/calls", post(control::call_tool))
        .route("/api/v1/peer/tools", get(peer::list_peer_tools))
        .route(
            "/api/v1/peer/tools/{tool_name}/calls",
            post(peer::call_peer_tool),
        )
        .route("/api/v1/peer/models", get(peer::list_peer_models))
        .route("/api/v1/baby/state", get(peer::baby_state))
        .route("/api/v1/peers", post(peer::add_peer))
        .route("/api/v1/peers/{node_id}", delete(peer::remove_peer))
        .route(
            "/api/v1/peers/{node_id}/baby/state",
            get(peer::peer_baby_state),
        )
        .route("/mcp", post(mcp::handle))
        .route("/v1/models", get(models::openai_models))
        .route("/v1/chat/completions", post(chat::chat_completions));

    let app = app.route("/api/v1/nodes", get(peer::list_nodes));

    let app = app.with_state(state);

    let addr = SocketAddr::new(host, port);
    tracing::info!("lociant-server listening on {addr}");
    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await?;
    Ok(())
}

fn load_headless_config(mut settings: Value) -> Value {
    let path = match std::env::var("LOCIANT_CONFIG") {
        Ok(path) if !path.is_empty() => path,
        _ => return settings,
    };
    let raw = match std::fs::read_to_string(&path) {
        Ok(raw) => raw,
        Err(error) => {
            tracing::warn!("headless config {path} unreadable: {error}");
            return settings;
        }
    };
    let config: Value = match serde_json::from_str(&raw) {
        Ok(config) => config,
        Err(error) => {
            tracing::warn!("headless config {path} parse failed: {error}");
            return settings;
        }
    };
    if let (Some(base), Some(config_object)) = (settings.as_object_mut(), config.as_object()) {
        for (key, value) in config_object {
            base.insert(key.clone(), value.clone());
        }
    }
    tracing::info!("loaded headless config from {path}");
    settings
}

fn peer_discovery_enabled(settings: &Value) -> bool {
    settings
        .get("peerDiscovery")
        .and_then(Value::as_bool)
        .unwrap_or(true)
}

async fn health() -> axum::Json<Value> {
    axum::Json(serde_json::json!({
        "status": "ok",
        "service": "lociant-server",
        "version": env!("CARGO_PKG_VERSION"),
    }))
}

#[cfg(test)]
mod tests {
    use super::peer_discovery_enabled;
    use serde_json::json;

    #[test]
    fn peer_discovery_config_is_boolean_and_defaults_on() {
        for (settings, expected) in [
            (json!({}), true),
            (json!({ "peerDiscovery": true }), true),
            (json!({ "peerDiscovery": false }), false),
            (json!({ "peerDiscovery": "false" }), true),
        ] {
            assert_eq!(peer_discovery_enabled(&settings), expected);
        }
    }
}
