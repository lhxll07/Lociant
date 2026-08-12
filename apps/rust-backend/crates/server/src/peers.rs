//! Peer-to-peer node discovery and remote capability sharing.
//!
//! Lociant nodes can advertise themselves over UDP broadcast (port 11435) and
//! watch for siblings, so discovery works on any LAN (Android included)
//! without mDNS/group-multicast support. Discovered peers are attached to
//! the shared
//! `ToolRegistry` (tools are called over the peer's `/api/v1/peer/*` routes,
//! and the provider enforces its own exposure policy — "the remote side
//! decides"). Peer model inference reuses the OpenAI-compatible
//! `/v1/chat/completions` route through the existing `CloudBackend`.

use std::collections::HashMap;
use std::net::{IpAddr, SocketAddr};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};

use async_trait::async_trait;
use lociant_agent::backend::{ChatBackend, TurnEvent, TurnOutcome};
use lociant_core::{ToolDescriptor, ToolResult};
use lociant_tools::{DeviceAdapter, ToolError, ToolRegistry};
use serde_json::{json, Value};
use tokio::sync::mpsc;

/// UDP discovery port (broadcast), separate from the HTTP service port.
pub const DISCOVERY_PORT: u16 = 11435;
/// Broadcast interval.
const ADVERTISE_INTERVAL: Duration = Duration::from_secs(5);

#[derive(Debug, Clone)]
pub struct PeerNode {
    pub id: String,
    pub name: String,
    pub platform: String,
    pub host: IpAddr,
    pub port: u16,
    pub last_seen: Instant,
}

type ToolsCache = Arc<Mutex<Option<(Instant, Vec<ToolDescriptor>)>>>;

/// Manages UDP advertisement, peer tracking and remote tool adapters.
pub struct PeerManager {
    pub self_id: String,
    pub self_name: String,
    port: u16,
    token: String,
    nodes: RwLock<HashMap<String, PeerNode>>,
    adapters: Mutex<HashMap<String, Arc<HttpPeerAdapter>>>,
    registry: Arc<ToolRegistry>,
    client: reqwest::blocking::Client,
}

impl PeerManager {
    pub fn new(
        registry: Arc<ToolRegistry>,
        self_id: String,
        self_name: String,
        token: String,
        port: u16,
    ) -> Self {
        // `reqwest::blocking::Client::build` creates and tears down an
        // internal Tokio runtime. Constructing it directly inside our
        // `#[tokio::main]` future panics while that temporary runtime is
        // dropped, so keep the complete blocking-client lifecycle setup on a
        // plain OS thread. Peer requests themselves already run on dedicated
        // threads or `spawn_blocking` call sites.
        let client = std::thread::spawn(|| {
            reqwest::blocking::Client::builder()
                .connect_timeout(Duration::from_secs(2))
                .timeout(Duration::from_secs(4))
                .build()
        })
        .join()
        .expect("peer HTTP client builder thread panicked")
        .expect("peer HTTP client build failed");
        PeerManager {
            self_id,
            self_name,
            port,
            token,
            nodes: RwLock::new(HashMap::new()),
            adapters: Mutex::new(HashMap::new()),
            registry,
            client,
        }
    }

    /// Starts peer services. Manual peers and their metadata refresher always
    /// work; UDP discovery is optional and defaults to enabled at the server
    /// configuration layer for backwards compatibility.
    pub fn start(self: &Arc<Self>, discovery: bool) {
        if discovery {
            // Advertisement: broadcast our identity periodically.
            let peers = self.clone();
            std::thread::spawn(move || {
                let socket = match std::net::UdpSocket::bind("0.0.0.0:0") {
                    Ok(socket) => socket,
                    Err(error) => {
                        tracing::error!("UDP advertise bind failed: {error}");
                        return;
                    }
                };
                let _ = socket.set_broadcast(true);
                let payload = json!({
                    "id": peers.self_id,
                    "name": peers.self_name,
                    "platform": std::env::consts::OS,
                    "port": peers.port,
                })
                .to_string();
                let targets = Self::subnet_broadcast_addrs();
                let targets = if targets.is_empty() {
                    vec![std::net::Ipv4Addr::BROADCAST]
                } else {
                    targets
                };
                loop {
                    for target in &targets {
                        match socket.send_to(payload.as_bytes(), (*target, DISCOVERY_PORT)) {
                            Ok(_) => {}
                            Err(error) => {
                                tracing::debug!(%target, "peer advertise failed: {error}")
                            }
                        }
                    }
                    std::thread::sleep(ADVERTISE_INTERVAL);
                }
            });

            // Discovery: listen for sibling broadcasts.
            let peers = self.clone();
            tokio::spawn(async move {
                let socket2_socket = match socket2::Socket::new(
                    socket2::Domain::IPV4,
                    socket2::Type::DGRAM,
                    Some(socket2::Protocol::UDP),
                ) {
                    Ok(socket) => socket,
                    Err(error) => {
                        tracing::error!("UDP discovery socket failed: {error}");
                        return;
                    }
                };
                let _ = socket2_socket.set_reuse_address(true);
                let _ = socket2_socket.set_nonblocking(true);
                let _ = socket2_socket.bind(
                    &format!("0.0.0.0:{DISCOVERY_PORT}")
                        .parse::<std::net::SocketAddr>()
                        .expect("static discovery addr")
                        .into(),
                );
                let std_socket: std::net::UdpSocket = socket2_socket.into();
                let socket = match tokio::net::UdpSocket::from_std(std_socket) {
                    Ok(socket) => socket,
                    Err(error) => {
                        tracing::error!("UDP discovery socket failed: {error}");
                        return;
                    }
                };
                let mut buf = [0u8; 2048];
                loop {
                    let Ok((size, addr)) = socket.recv_from(&mut buf).await else {
                        continue;
                    };
                    if let Ok(payload) = serde_json::from_slice::<Value>(&buf[..size]) {
                        peers.handle_discovery_packet(addr, payload);
                    }
                }
            });

            // Heartbeat cleanup: drop peers that stopped broadcasting.
            let peers = self.clone();
            tokio::spawn(async move {
                let mut interval = tokio::time::interval(Duration::from_secs(15));
                loop {
                    interval.tick().await;
                    peers.reap_stale(Duration::from_secs(45));
                }
            });

            tracing::info!("peer discovery enabled (UDP broadcast :{})", DISCOVERY_PORT);
        } else {
            tracing::info!("peer discovery disabled; manual peers remain available");
        }

        // Background refresher: keep every peer's tool metadata warm so the
        // request path only ever hits the cache. Primes immediately, then
        // runs every TTL. Blocking I/O lives in this dedicated thread.
        let peers = self.clone();
        std::thread::spawn(move || loop {
            let adapters = {
                let adapters = peers.adapters.lock().expect("adapters lock");
                adapters.values().cloned().collect::<Vec<_>>()
            };
            for adapter in adapters {
                adapter.refresh_tools();
            }
            std::thread::sleep(PEER_TOOLS_TTL);
        });
    }

    fn handle_discovery_packet(&self, addr: SocketAddr, payload: Value) {
        let id = payload.get("id").and_then(Value::as_str).unwrap_or("");
        if id.is_empty() || id == self.self_id {
            return;
        }
        let node = PeerNode {
            id: id.to_owned(),
            name: payload
                .get("name")
                .and_then(Value::as_str)
                .unwrap_or(id)
                .to_owned(),
            platform: payload
                .get("platform")
                .and_then(Value::as_str)
                .unwrap_or_default()
                .to_owned(),
            host: addr.ip(),
            port: payload.get("port").and_then(Value::as_u64).unwrap_or(11434) as u16,
            last_seen: Instant::now(),
        };
        self.upsert_peer(node);
    }

    /// Enumerates per-interface subnet broadcast addresses from
    /// `/proc/net/route` (Linux/Android), so announcements reach every LAN
    /// interface instead of just the default-route one.
    fn subnet_broadcast_addrs() -> Vec<std::net::Ipv4Addr> {
        let Ok(content) = std::fs::read_to_string("/proc/net/route") else {
            return Vec::new();
        };
        let mut addrs = Vec::new();
        for line in content.lines().skip(1) {
            let fields: Vec<&str> = line.split_whitespace().collect();
            if fields.len() < 8 || fields[0] == "lo" {
                continue;
            }
            let Ok(dest) = u32::from_str_radix(fields[1], 16) else {
                continue;
            };
            let Ok(mask) = u32::from_str_radix(fields[7], 16) else {
                continue;
            };
            if dest == 0 && mask == 0 {
                continue; // default route: broadcast is 255.255.255.255 anyway
            }
            let broadcast = dest.swap_bytes() | !mask.swap_bytes();
            addrs.push(std::net::Ipv4Addr::from(broadcast));
        }
        addrs
    }

    /// Removes peers that stopped broadcasting (heartbeat timeout).
    pub fn reap_stale(&self, timeout: Duration) {
        let stale = {
            let nodes = self.nodes.read().expect("nodes lock");
            nodes
                .values()
                .filter(|node| node.last_seen.elapsed() > timeout)
                .map(|node| node.id.clone())
                .collect::<Vec<_>>()
        };
        for id in stale {
            if !self
                .nodes
                .read()
                .expect("nodes lock")
                .get(&id)
                .map(|node| node.platform == "manual")
                .unwrap_or(false)
            {
                self.remove_peer(&id);
            }
        }
    }

    fn upsert_peer(&self, node: PeerNode) {
        let is_new = {
            let nodes = self.nodes.read().expect("nodes lock");
            !nodes.contains_key(&node.id)
        };
        {
            let mut nodes = self.nodes.write().expect("nodes lock");
            nodes.insert(node.id.clone(), node.clone());
        }
        if is_new {
            let adapter = Arc::new(HttpPeerAdapter {
                base_url: format!("http://{}:{}", node.host, node.port),
                token: self.token.clone(),
                client: self.client.clone(),
                tools_cache: Arc::new(Mutex::new(None)),
            });
            self.registry.add_adapter(adapter.clone());
            let mut adapters = self.adapters.lock().expect("adapters lock");
            adapters.insert(node.id.clone(), adapter);
            tracing::info!(
                "peer discovered: {} ({}) at {}:{}",
                node.name,
                node.platform,
                node.host,
                node.port
            );
        }
    }

    pub fn remove_peer(&self, id: &str) {
        let mut nodes = self.nodes.write().expect("nodes lock");
        nodes.remove(id);
        let adapter = self.adapters.lock().expect("adapters lock").remove(id);
        if let Some(adapter) = adapter {
            let adapter: Arc<dyn DeviceAdapter> = adapter;
            self.registry.remove_adapter(&adapter);
            tracing::info!("peer left: {id}");
        }
    }

    pub fn nodes(&self) -> Vec<PeerNode> {
        let nodes = self.nodes.read().expect("nodes lock");
        nodes.values().cloned().collect()
    }

    /// Manually registers a peer (host:port) without mDNS, so nodes on any
    /// platform can join by address.
    pub fn add_manual_peer(&self, host: String, port: u16, name: Option<String>) {
        let id = format!("{host}:{port}");
        if id == format!("{}:{}", self.self_id, self.port) {
            return;
        }
        let host_ip = host
            .parse::<IpAddr>()
            .unwrap_or(IpAddr::V4(std::net::Ipv4Addr::LOCALHOST));
        let node = PeerNode {
            id: id.clone(),
            name: name.unwrap_or_else(|| id.clone()),
            platform: "manual".to_owned(),
            host: host_ip,
            port,
            last_seen: Instant::now(),
        };
        self.upsert_peer(node);
        tracing::info!("peer added manually: {id}");
    }

    pub fn node(&self, id: &str) -> Option<PeerNode> {
        self.nodes.read().expect("nodes lock").get(id).cloned()
    }

    /// OpenAI-compatible base URL for a peer node (for chat forwarding).
    pub fn peer_base_url(&self, id: &str) -> Option<(String, String)> {
        let node = self.node(id)?;
        Some((
            format!("http://{}:{}/v1", node.host, node.port),
            self.token.clone(),
        ))
    }

    /// Models exposed by peers as `peer:<nodeId>:<modelId>`.
    pub fn peer_models(&self) -> Vec<Value> {
        let mut models = Vec::new();
        for node in self.nodes() {
            let mut request = self.client.get(format!(
                "http://{}:{}/api/v1/peer/models",
                node.host, node.port
            ));
            if !self.token.is_empty() {
                request = request.bearer_auth(&self.token);
            }
            let Ok(response) = request.send() else {
                tracing::warn!("peer models fetch failed for {}", node.id);
                continue;
            };
            let Ok(body) = response.json::<Value>() else {
                tracing::warn!("peer models parse failed for {}", node.id);
                continue;
            };
            if let Some(list) = body.get("models").and_then(Value::as_array) {
                for model in list {
                    if let Some(id) = model.get("id").and_then(Value::as_str) {
                        models.push(json!({
                            "id": format!("peer:{}:{}", node.id, id),
                            "name": format!("{} · {}", node.name, id),
                            "owned_by": "peer",
                            "peer": true,
                        }));
                    }
                }
            }
        }
        models
    }
}

/// Remote tool adapter: calls a peer node's `/api/v1/peer/*` routes with the
/// shared peer token. The provider enforces its own exposure policy.
#[derive(Clone)]
pub struct HttpPeerAdapter {
    base_url: String,
    token: String,
    client: reqwest::blocking::Client,
    tools_cache: ToolsCache,
}

/// Remote tool metadata is slow-changing; cache it so the agent loop and the
/// control plane never wait on a peer round trip for every request.
const PEER_TOOLS_TTL: Duration = Duration::from_secs(15);

/// Forwards a chat turn to a peer node, stripping the `peer:<node>:`
/// prefix from the model name before the OpenAI-compatible request is sent.
pub struct PeerChatBackend {
    pub inner: lociant_agent::backend::CloudBackend,
    pub model_id: String,
}

#[async_trait]
impl ChatBackend for PeerChatBackend {
    async fn stream_turn(&self, body: &Value, events: mpsc::Sender<TurnEvent>) -> TurnOutcome {
        let mut body = body.clone();
        body["model"] = json!(self.model_id);
        self.inner.stream_turn(&body, events).await
    }

    async fn complete_turn(&self, body: &Value) -> TurnOutcome {
        let mut body = body.clone();
        body["model"] = json!(self.model_id);
        self.inner.complete_turn(&body).await
    }
}

impl DeviceAdapter for HttpPeerAdapter {
    fn is_peer(&self) -> bool {
        true
    }

    fn tools(&self) -> Vec<ToolDescriptor> {
        if let Some((at, cached)) = self
            .tools_cache
            .lock()
            .expect("peer tools cache lock")
            .as_ref()
        {
            if at.elapsed() < PEER_TOOLS_TTL {
                return cached.clone();
            }
        }
        self.refresh_tools()
    }

    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError> {
        let mut request = self
            .client
            .post(format!("{}/api/v1/peer/tools/{name}/calls", self.base_url));
        if !self.token.is_empty() {
            request = request.bearer_auth(&self.token);
        }
        let response = request
            .json(&json!({ "arguments": arguments }))
            .send()
            .map_err(|error| ToolError::Adapter(format!("peer {name} call failed: {error}")))?;
        let status = response.status();
        let body = response
            .json::<Value>()
            .map_err(|_| ToolError::Adapter(format!("peer {name} bad response")))?;
        if !status.is_success() {
            return Err(ToolError::Adapter(
                body.get("detail")
                    .and_then(Value::as_str)
                    .unwrap_or("peer call failed")
                    .to_owned(),
            ));
        }
        serde_json::from_value(body.get("data").cloned().unwrap_or_default())
            .map_err(|_| ToolError::Adapter(format!("peer {name} result parse failed")))
    }
}

impl HttpPeerAdapter {
    /// Fetches the peer's tool list and stores the result — including
    /// failures, so an offline peer cannot stall request paths repeatedly.
    fn refresh_tools(&self) -> Vec<ToolDescriptor> {
        let tools: Vec<ToolDescriptor> = self.fetch_tools().unwrap_or_default();
        if let Ok(mut cache) = self.tools_cache.lock() {
            cache.replace((Instant::now(), tools.clone()));
        }
        tools
    }

    fn fetch_tools(&self) -> Option<Vec<ToolDescriptor>> {
        let mut request = self
            .client
            .get(format!("{}/api/v1/peer/tools", self.base_url));
        if !self.token.is_empty() {
            request = request.bearer_auth(&self.token);
        }
        let Ok(response) = request.send() else {
            return None;
        };
        let Ok(body) = response.json::<Value>() else {
            return None;
        };
        serde_json::from_value(
            body.get("data")
                .cloned()
                .unwrap_or(Value::Array(Vec::new())),
        )
        .ok()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use lociant_tools::NoopDevice;

    fn manager(id: &str) -> PeerManager {
        let registry = Arc::new(ToolRegistry::new(Box::new(NoopDevice)));
        PeerManager::new(
            registry,
            id.to_owned(),
            "Test node".to_owned(),
            String::new(),
            11434,
        )
    }

    #[tokio::test]
    async fn manager_can_be_created_inside_tokio_runtime() {
        let manager = manager("test-node");
        assert_eq!(manager.self_id, "test-node");
    }

    #[test]
    fn manual_peer_is_available_without_discovery() {
        let manager = manager("self");
        manager.add_manual_peer("192.0.2.10".to_owned(), 11434, Some("test".to_owned()));
        let node = manager.node("192.0.2.10:11434").expect("manual peer");
        assert_eq!(node.platform, "manual");
        assert_eq!(node.name, "test");
        assert_eq!(
            manager.peer_base_url(&node.id).expect("peer URL").0,
            "http://192.0.2.10:11434/v1"
        );
    }
}
