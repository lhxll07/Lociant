//! Shared domain types for the Lociant Rust backend.
//!
//! Field names intentionally match the JSON contract consumed by the Flutter
//! UI (camelCase, same shapes as the Android runtime's `runtimeState()`).
//! Parsing on the Dart side is defensive, so unknown fields are safe.

use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSummary {
    pub id: String,
    pub title: String,
    pub model_id: String,
    pub updated_at: i64,
    pub message_count: u32,
    pub last_role: String,
    pub last_text: String,
}

/// A persisted chat message inside a session.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StoredMessage {
    pub id: i64,
    pub role: String,
    pub text: String,
    /// Lociant-specific side channel (e.g. `{"reasoning": "…"}`) rendered by
    /// the Flutter UI; never sent back to upstream providers.
    pub content_json: Value,
    pub created_at: i64,
}

/// A tool a device adapter can execute, with the same policy flags the
/// Android runtime already exposes. `exposure` levels are `read`, `sensor`
/// and `action`; `remote_allowed` controls HTTP/MCP callers, while `local`
/// means the tool only runs inside the owning process.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolDescriptor {
    pub name: String,
    pub description: String,
    pub arguments: Value,
    #[serde(default = "default_exposure")]
    pub exposure: String,
    #[serde(default)]
    pub local: bool,
    #[serde(default)]
    pub remote_allowed: bool,
    #[serde(default)]
    pub requires_activity: bool,
    #[serde(default)]
    pub side_effect: bool,
    #[serde(default)]
    pub destructive: bool,
    #[serde(default)]
    pub open_world: bool,
}

fn default_exposure() -> String {
    "read".to_owned()
}

/// The single execution envelope returned by every tool, regardless of
/// whether the caller is the control API, MCP or a local runtime component.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolResult {
    pub ok: bool,
    pub content: String,
    #[serde(default)]
    pub structured: Value,
    #[serde(default)]
    pub error: Option<String>,
}

impl ToolResult {
    pub fn ok(content: impl Into<String>) -> Self {
        ToolResult {
            ok: true,
            content: content.into(),
            structured: Value::Null,
            error: None,
        }
    }

    pub fn err(error: impl Into<String>) -> Self {
        ToolResult {
            ok: false,
            content: String::new(),
            structured: Value::Null,
            error: Some(error.into()),
        }
    }
}

/// A tool call emitted by a local model runtime.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelToolCall {
    pub id: String,
    pub name: String,
    pub arguments: String,
}

/// A locally installed model, as reported in `GET /api/v1/models`.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledModel {
    pub id: String,
    pub name: String,
    pub runtime: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub installed_at: i64,
}

/// The full runtime snapshot reported by `GET /api/v1/runtime` (and, on
/// Android, `runtimeState()` on the platform channel). All fields have
/// defaults so a new backend can start with a partial JSON body.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct RuntimeState {
    pub running: bool,
    pub starting: bool,
    pub port: u16,
    pub lan_url: String,
    pub url: String,
    pub auth_token: String,
    pub model_id: String,
    pub model_loaded: bool,
    pub model_loading: bool,
    pub max_output_tokens: u32,
    pub hard_max_output_tokens: u32,
    pub tool_exposure: String,
    pub auto_start: bool,
    pub request_count: u64,
    pub recent_requests: Vec<Value>,
    pub last_error: String,
    pub message: String,
    pub camera_permission_granted: bool,
    pub notification_permission_granted: bool,
    pub window_allowed: bool,
    pub window_visible: bool,
    pub window_state: String,
    pub window_auto_show: bool,
    pub battery_optimization_ignored: bool,
    pub accessibility_permission_granted: bool,
    pub vision: Value,
    pub device: Value,
}

impl Default for RuntimeState {
    fn default() -> Self {
        RuntimeState {
            running: false,
            starting: false,
            port: 11434,
            lan_url: String::new(),
            url: String::new(),
            auth_token: String::new(),
            model_id: String::new(),
            model_loaded: false,
            model_loading: false,
            max_output_tokens: 512,
            hard_max_output_tokens: 32768,
            tool_exposure: "action".into(),
            auto_start: false,
            request_count: 0,
            recent_requests: Vec::new(),
            last_error: String::new(),
            message: "runtime stopped".into(),
            camera_permission_granted: false,
            notification_permission_granted: false,
            window_allowed: false,
            window_visible: false,
            window_state: "hidden".into(),
            window_auto_show: false,
            battery_optimization_ignored: false,
            accessibility_permission_granted: false,
            vision: json!({}),
            device: json!({}),
        }
    }
}
