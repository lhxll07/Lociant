//! Peer-to-peer node discovery and remote capability sharing.
//!
//! Every Lociant node advertises itself over mDNS (`_lociant._tcp.local.`)
//! and watches for siblings. Discovered peers are attached to the shared
//! `ToolRegistry` (tools are called over the peer's `/api/v1/peer/*` routes,
//! and the provider enforces its own exposure policy — "the remote side
//! decides"). Peer model inference reuses the OpenAI-compatible
//! `/v1/chat/completions` route through the existing `CloudBackend`.

use std::collections::HashMap;
use std::net::IpAddr;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};

use lociant_core::{ToolDescriptor, ToolResult};
use lociant_tools::{DeviceAdapter, ToolError, ToolRegistry};
use mdns_sd::{ServiceDaemon, ServiceEvent, ServiceInfo};
use async_trait::async_trait;
use lociant_agent::backend::{ChatBackend, TurnEvent, TurnOutcome};
use serde_json::{json, Value};
use tokio::sync::mpsc;

pub const SERVICE_TYPE: &str = "_lociant._tcp.local.";

#[derive(Debug, Clone)]
pub struct PeerNode {
    pub id: String,
    pub name: String,
    pub platform: String,
    pub host: IpAddr,
    pub port: u16,
    pub last_seen: Instant,
}

/// Manages mDNS advertisement, peer tracking and remote tool adapters.
pub struct PeerManager {
    pub self_id: String,
    pub self_name: String,
    port: u16,
    token: String,
    nodes: RwLock<HashMap<String, PeerNode>>,
    adapters: Mutex<HashMap<String, Arc<HttpPeerAdapter>>>,
    registry: Arc<ToolRegistry>,
    client: reqwest::blocking::Client,
    _daemon: ServiceDaemon,
}

impl PeerManager {
    pub fn new(
        registry: Arc<ToolRegistry>,
        self_id: String,
        self_name: String,
        token: String,
        port: u16,
    ) -> Result<Self, mdns_sd::Error> {
        let daemon = ServiceDaemon::new()?;
        let manager = PeerManager {
            self_id,
            self_name,
            port,
            token,
            nodes: RwLock::new(HashMap::new()),
            adapters: Mutex::new(HashMap::new()),
            registry,
            client: reqwest::blocking::Client::builder()
                .timeout(Duration::from_secs(5))
                .build()
                .unwrap_or_default(),
            _daemon: daemon,
        };
        Ok(manager)
    }

    /// Starts advertising this node and browsing for siblings. Runs the
    /// mDNS event loop on a background task; `peers` keeps the daemon alive.
    pub fn start(self: &Arc<Self>, ip: IpAddr) {
        let mut properties = HashMap::new();
        properties.insert("id".to_owned(), self.self_id.clone());
        properties.insert("name".to_owned(), self.self_name.clone());
        properties.insert("platform".to_owned(), std::env::consts::OS.to_owned());
        let info = ServiceInfo::new(
            SERVICE_TYPE,
            &self.self_id,
            &format!("{}.local.", self.self_id),
            ip,
            self.port,
            properties,
        )
        .expect("valid mDNS service info");
        if let Err(error) = self._daemon.register(info) {
            tracing::warn!("mDNS register failed: {error}");
            return;
        }
        let receiver = match self._daemon.browse(SERVICE_TYPE) {
            Ok(receiver) => receiver,
            Err(error) => {
                tracing::warn!("mDNS browse failed: {error}");
                return;
            }
        };
        let peers = self.clone();
        tokio::spawn(async move {
            while let Ok(event) = receiver.recv_async().await {
                peers.handle_event(event);
            }
        });
        tracing::info!(
            "peer discovery enabled (mDNS {}:{})",
            ip,
            self.port
        );
    }

    fn handle_event(&self, event: ServiceEvent) {
        match event {
            ServiceEvent::ServiceResolved(info) => {
                let props = info.get_properties();
                let id = props.get_property_val_str("id").unwrap_or_default().to_owned();
                if id.is_empty() || id == self.self_id {
                    return;
                }
                let Some(ip) = info.get_addresses_v4().into_iter().next() else {
                    return;
                };
                let node = PeerNode {
                    id: id.clone(),
                    name: props
                        .get_property_val_str("name")
                        .unwrap_or(&id)
                        .to_owned(),
                    platform: props
                        .get_property_val_str("platform")
                        .unwrap_or_default()
                        .to_owned(),
                    host: IpAddr::V4(ip),
                    port: info.get_port(),
                    last_seen: Instant::now(),
                };
                self.upsert_peer(node);
            }
            ServiceEvent::ServiceRemoved(_, fullname) => {
                let id = fullname
                    .split('.')
                    .next()
                    .unwrap_or_default()
                    .to_owned();
                self.remove_peer(&id);
            }
            _ => {}
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

    fn remove_peer(&self, id: &str) {
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
            let Ok(response) = self
                .client
                .get(format!(
                    "http://{}:{}/api/v1/peer/models",
                    node.host, node.port
                ))
                .bearer_auth(&self.token)
                .send()
            else {
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
}

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
    fn tools(&self) -> Vec<ToolDescriptor> {
        let Ok(response) = self
            .client
            .get(format!("{}/api/v1/peer/tools", self.base_url))
            .bearer_auth(&self.token)
            .send()
        else {
            return Vec::new();
        };
        let Ok(body) = response.json::<Value>() else {
            return Vec::new();
        };
        serde_json::from_value(body.get("data").cloned().unwrap_or(Value::Array(Vec::new())))
            .unwrap_or_default()
    }

    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError> {
        let response = self
            .client
            .post(format!("{}/api/v1/peer/tools/{name}/calls", self.base_url))
            .bearer_auth(&self.token)
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
