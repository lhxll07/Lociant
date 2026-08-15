//! IPC device adapter: talks to the Kotlin device layer (Android) over a
//! localhost TCP JSON protocol. Rust stays the single policy owner
//! (exposure + remote_allowed in `ToolRegistry`); this side only discovers
//! and executes device capabilities.

use std::io::{BufRead, BufReader, Write};
use std::net::TcpStream;
use std::time::Duration;

use lociant_core::{ToolDescriptor, ToolResult};
use lociant_tools::{DeviceAdapter, ToolError};
use serde_json::{json, Value};

pub struct IpcDeviceAdapter {
    pub port: u16,
    pub token: String,
}

impl IpcDeviceAdapter {
    pub const TOKEN_ENV: &'static str = "LOCIANT_DEVICE_TOKEN";
    pub const PORT_ENV: &'static str = "LOCIANT_DEVICE_PORT";

    fn request(&self, payload: &Value) -> Result<Value, String> {
        let stream = TcpStream::connect(("127.0.0.1", self.port))
            .map_err(|error| format!("device adapter connect failed: {error}"))?;
        stream.set_read_timeout(Some(Duration::from_secs(10))).ok();
        stream.set_write_timeout(Some(Duration::from_secs(5))).ok();
        let mut stream = stream;
        let mut line = payload.to_string();
        line.push('\n');
        stream
            .write_all(line.as_bytes())
            .map_err(|error| format!("device adapter write failed: {error}"))?;
        let mut response = String::new();
        BufReader::new(stream)
            .read_line(&mut response)
            .map_err(|error| format!("device adapter read failed: {error}"))?;
        serde_json::from_str(&response)
            .map_err(|error| format!("device adapter response parse failed: {error}"))
    }

    /// Local model specs from the Kotlin model manager.
    pub fn models(&self) -> Vec<Value> {
        let request = json!({ "token": self.token, "method": "models.list" });
        let Ok(response) = self.request(&request) else {
            return Vec::new();
        };
        if response.get("ok").and_then(Value::as_bool) != Some(true) {
            return Vec::new();
        }
        response
            .get("models")
            .and_then(Value::as_array)
            .cloned()
            .unwrap_or_default()
    }

    pub fn has_model(&self, model_id: &str) -> bool {
        self.models().iter().any(|model| {
            model.get("id").and_then(Value::as_str) == Some(model_id)
                || model.get("name").and_then(Value::as_str) == Some(model_id)
        })
    }

    /// Pushes Rust-owned settings down to the Kotlin device layer.
    pub fn sync_settings(&self, settings: &Value) -> Result<(), String> {
        let request = json!({
            "token": self.token,
            "method": "settings.sync",
            "settings": settings,
        });
        let response = self.request(&request)?;
        if response.get("ok").and_then(Value::as_bool) == Some(true) {
            Ok(())
        } else {
            Err(response
                .get("error")
                .and_then(Value::as_str)
                .unwrap_or("settings sync rejected")
                .to_owned())
        }
    }

    /// Drops the Kotlin model snapshot so the next `models.list` rescan sees
    /// installs/deletes performed by the Rust backend.
    pub fn invalidate_models(&self) -> Result<(), String> {
        let request = json!({ "token": self.token, "method": "models.invalidate" });
        let response = self.request(&request)?;
        if response.get("ok").and_then(Value::as_bool) == Some(true) {
            Ok(())
        } else {
            Err(response
                .get("error")
                .and_then(Value::as_str)
                .unwrap_or("model cache invalidation rejected")
                .to_owned())
        }
    }
}

impl DeviceAdapter for IpcDeviceAdapter {
    fn tools(&self) -> Vec<ToolDescriptor> {
        let request = json!({ "token": self.token, "method": "tools.list" });
        let Ok(response) = self.request(&request) else {
            return Vec::new();
        };
        if response.get("ok").and_then(Value::as_bool) != Some(true) {
            return Vec::new();
        }
        response
            .get("tools")
            .and_then(Value::as_array)
            .map(|tools| tools.iter().filter_map(kotlin_descriptor).collect())
            .unwrap_or_default()
    }

    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError> {
        let request = json!({
            "token": self.token,
            "method": "tools.call",
            "name": name,
            "arguments": arguments,
        });
        let response = self.request(&request).map_err(ToolError::Adapter)?;
        Ok(map_result(response))
    }
}

fn kotlin_descriptor(raw: &Value) -> Option<ToolDescriptor> {
    let function = raw.get("function")?;
    let policy = raw.get("x_policy").cloned().unwrap_or_else(|| json!({}));
    let flag = |key: &str| policy.get(key).and_then(Value::as_bool).unwrap_or(false);
    Some(ToolDescriptor {
        name: function
            .get("name")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned(),
        description: function
            .get("description")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_owned(),
        arguments: function
            .get("parameters")
            .cloned()
            .unwrap_or_else(|| json!({ "type": "object", "properties": {} })),
        exposure: raw
            .get("x_lociant_level")
            .and_then(Value::as_str)
            .unwrap_or("read")
            .to_owned(),
        local: flag("local"),
        remote_allowed: flag("remoteAllowed"),
        requires_activity: flag("requiresActivity"),
        side_effect: flag("sideEffect"),
        destructive: flag("destructive"),
        open_world: flag("openWorld"),
    })
}

fn map_result(response: Value) -> ToolResult {
    let ok = response.get("ok").and_then(Value::as_bool).unwrap_or(false);
    let result = response.get("result").cloned().unwrap_or(Value::Null);
    let content = result
        .get("content")
        .and_then(Value::as_str)
        .map(str::to_owned)
        .unwrap_or_else(|| result.to_string());
    let error = if ok {
        None
    } else {
        Some(
            response
                .get("error")
                .and_then(|error| error.get("message"))
                .and_then(Value::as_str)
                .unwrap_or("tool failed")
                .to_owned(),
        )
    };
    ToolResult {
        ok,
        content,
        structured: result,
        error,
    }
}
