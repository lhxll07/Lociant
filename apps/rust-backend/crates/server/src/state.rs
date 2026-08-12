use std::collections::HashMap;
use std::net::{IpAddr, UdpSocket};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use lociant_rkllm::Rkllm;
use lociant_store::Store;
use lociant_tools::ToolRegistry;
use serde_json::Value;

use crate::baby::BabyMonitor;
use crate::catalog::CatalogEntry;
use crate::device::IpcDeviceAdapter;
use crate::models::InstallJob;
use crate::peers::PeerManager;

#[derive(Clone)]
pub struct AppState {
    pub store: Arc<Store>,
    pub settings: Arc<Mutex<Value>>,
    pub port: u16,
    pub http: reqwest::Client,
    pub download_http: reqwest::Client,
    pub tools: Arc<ToolRegistry>,
    pub device: Option<Arc<IpcDeviceAdapter>>,
    pub catalog: Arc<Vec<CatalogEntry>>,
    pub models_dir: PathBuf,
    pub installs: Arc<Mutex<HashMap<String, InstallJob>>>,
    pub rkllm: Option<Arc<Rkllm>>,
    pub peers: Option<Arc<PeerManager>>,
    pub baby: Option<Arc<dyn BabyMonitor>>,
    pub baby_cache: Arc<Mutex<HashMap<String, (std::time::Instant, Value)>>>,
    pub tools_cache: Arc<Mutex<HashMap<String, (std::time::Instant, serde_json::Value)>>>,
}

impl AppState {
    pub fn settings_snapshot(&self) -> Value {
        self.settings.lock().expect("settings lock").clone()
    }

    pub fn public_settings_snapshot(&self) -> Value {
        redact_settings(self.settings_snapshot())
    }

    /// Cached peer baby snapshot (1s TTL).
    pub fn baby_cache(&self, node_id: &str) -> Option<Value> {
        let cache = self.baby_cache.lock().expect("baby cache lock");
        cache.get(node_id).and_then(|(at, body)| {
            if at.elapsed() < std::time::Duration::from_secs(1) {
                Some(body.clone())
            } else {
                None
            }
        })
    }

    pub fn set_baby_cache(&self, node_id: &str, body: Value) {
        let mut cache = self.baby_cache.lock().expect("baby cache lock");
        cache.insert(node_id.to_owned(), (std::time::Instant::now(), body));
    }

    /// Cached tool list (5s TTL), keyed by exposure so a policy change is
    /// never served stale descriptors across levels.
    pub fn tools_cached(&self, exposure: &str) -> Option<serde_json::Value> {
        let cache = self.tools_cache.lock().expect("tools cache lock");
        cache.get(exposure).and_then(|(at, body)| {
            if at.elapsed() < std::time::Duration::from_secs(5) {
                Some(body.clone())
            } else {
                None
            }
        })
    }

    pub fn set_tools_cache(&self, exposure: &str, tools: Vec<lociant_core::ToolDescriptor>) {
        let mut cache = self.tools_cache.lock().expect("tools cache lock");
        let body = serde_json::to_value(tools).unwrap_or_default();
        cache.insert(exposure.to_owned(), (std::time::Instant::now(), body));
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
        merge_settings_value(&mut current, patch);
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

fn merge_settings_value(current: &mut Value, patch: &Value) {
    let (Some(current_obj), Some(patch_obj)) = (current.as_object_mut(), patch.as_object()) else {
        return;
    };
    let clear_cloud_key = patch_obj
        .get("clearCloudApiKey")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    for (key, value) in patch_obj {
        if key == "clearCloudApiKey" || key == "generateAuthToken" {
            continue;
        }
        if key == "cloudApiKey" && value.as_str() == Some("") && !clear_cloud_key {
            continue;
        }
        current_obj.insert(key.clone(), value.clone());
    }
    if clear_cloud_key {
        current_obj.insert("cloudApiKey".to_owned(), Value::String(String::new()));
    }
}

fn redact_settings(mut settings: Value) -> Value {
    if let Some(object) = settings.as_object_mut() {
        for key in ["authToken", "peerToken", "cloudApiKey"] {
            if object.contains_key(key) {
                object.insert(key.to_owned(), Value::String(String::new()));
            }
        }
    }
    settings
}

#[cfg(test)]
mod tests {
    use super::{merge_settings_value, redact_settings};
    use serde_json::json;

    #[test]
    fn public_settings_never_expose_secrets() {
        let settings = redact_settings(json!({
            "authToken": "api-secret",
            "peerToken": "peer-secret",
            "cloudApiKey": "cloud-secret",
            "cloudModel": "model",
        }));
        assert_eq!(settings["authToken"], "");
        assert_eq!(settings["peerToken"], "");
        assert_eq!(settings["cloudApiKey"], "");
        assert_eq!(settings["cloudModel"], "model");
    }

    #[test]
    fn blank_cloud_key_preserves_secret_until_explicit_clear() {
        let mut settings = json!({ "cloudApiKey": "secret", "cloudModel": "old" });
        merge_settings_value(
            &mut settings,
            &json!({ "cloudApiKey": "", "cloudModel": "new" }),
        );
        assert_eq!(settings["cloudApiKey"], "secret");
        assert_eq!(settings["cloudModel"], "new");

        merge_settings_value(&mut settings, &json!({ "clearCloudApiKey": true }));
        assert_eq!(settings["cloudApiKey"], "");
        assert!(settings.get("clearCloudApiKey").is_none());
    }
}
