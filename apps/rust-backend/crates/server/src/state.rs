use std::collections::HashMap;
use std::net::{IpAddr, UdpSocket};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use lociant_store::Store;
use lociant_tools::ToolRegistry;
use serde_json::Value;

use crate::catalog::CatalogEntry;
use crate::device::IpcDeviceAdapter;
use crate::models::InstallJob;

#[derive(Clone)]
pub struct AppState {
    pub store: Arc<Store>,
    pub settings: Arc<Mutex<Value>>,
    pub port: u16,
    pub http: reqwest::Client,
    pub tools: Arc<ToolRegistry>,
    pub device: Option<Arc<IpcDeviceAdapter>>,
    pub catalog: Arc<Vec<CatalogEntry>>,
    pub models_dir: PathBuf,
    pub installs: Arc<Mutex<HashMap<String, InstallJob>>>,
}

impl AppState {
    pub fn settings_snapshot(&self) -> Value {
        self.settings.lock().expect("settings lock").clone()
    }

    pub fn auth_token(&self) -> String {
        self.settings
            .lock()
            .expect("settings lock")
            .get("authToken")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned()
    }

    /// Merges a settings patch into the current settings (top-level fields)
    /// and persists the result to SQLite before returning it.
    pub fn merge_settings(&self, patch: &Value) -> Value {
        let mut current = self.settings.lock().expect("settings lock");
        if let (Some(current_obj), Some(patch_obj)) = (current.as_object_mut(), patch.as_object()) {
            for (key, value) in patch_obj {
                current_obj.insert(key.clone(), value.clone());
            }
        } else if patch.is_object() {
            *current = patch.clone();
        }
        let next = current.clone();
        drop(current);
        if let Err(error) = self.store.set_json("settings", &next) {
            tracing::warn!("persist settings failed: {error}");
        }
        next
    }

    /// Best-effort LAN address for the runtime summary. Uses the same
    /// "connect without sending" trick as many local servers to discover the
    /// interface that would carry outbound traffic.
    pub fn lan_ip(&self) -> Option<IpAddr> {
        let socket = UdpSocket::bind("0.0.0.0:0").ok()?;
        socket.connect("8.8.8.8:80").ok()?;
        socket.local_addr().ok().map(|addr| addr.ip())
    }
}
