//! Tool contract and registry shared by the agent loop, the control API and
//! (later) MCP.
//!
//! The server never talks to a device directly: it talks to a
//! [`DeviceAdapter`]. On Android the adapter will be a thin IPC client to the
//! Kotlin device layer (accessibility, sensors, window, camera, local
//! inference); on Linux it will be the desktop implementation. Both must
//! speak the same `ToolDescriptor` / `ToolResult` JSON contract.

use std::fmt;
use std::sync::{Arc, RwLock};

use lociant_core::{ToolDescriptor, ToolResult};
use serde_json::Value;

#[derive(Debug)]
pub enum ToolError {
    /// No adapter tool with this name exists.
    Unavailable(String),
    /// The tool exists but the caller is not allowed to run it.
    NotAllowed(String),
    /// The adapter rejected or failed the call.
    Adapter(String),
}

impl fmt::Display for ToolError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ToolError::Unavailable(message) => write!(f, "tool unavailable: {message}"),
            ToolError::NotAllowed(message) => write!(f, "tool not allowed: {message}"),
            ToolError::Adapter(message) => write!(f, "tool call failed: {message}"),
        }
    }
}

impl std::error::Error for ToolError {}

/// Platform capability provider. Implementations must be cheap to clone and
/// thread-safe; tool calls may block (native inference, accessibility
/// round-trips) so the agent loop runs them off the async runtime.
pub trait DeviceAdapter: Send + Sync {
    fn tools(&self) -> Vec<ToolDescriptor>;
    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError>;
    /// Peer adapters forward to a remote node; the peer plane excludes them
    /// so tool listings never recurse across nodes.
    fn is_peer(&self) -> bool {
        false
    }
}

/// No-op adapter used until a real device layer exists (Linux desktop MVP).
pub struct NoopDevice;

impl DeviceAdapter for NoopDevice {
    fn tools(&self) -> Vec<ToolDescriptor> {
        Vec::new()
    }

    fn call(&self, name: &str, _arguments: Value) -> Result<ToolResult, ToolError> {
        Err(ToolError::Unavailable(name.to_owned()))
    }
}

/// Owns the tool policy. All execution paths (agent loop, control API, MCP)
/// go through this type so metadata can never diverge from enforcement.
///
/// Adapters are dynamic so peers (other Lociant nodes on the LAN) can be
/// attached and removed as they join and leave; every adapter contributes its
/// tools, and a call is routed to the first adapter that provides the tool.
pub struct ToolRegistry {
    adapters: RwLock<Vec<Arc<dyn DeviceAdapter>>>,
}

impl ToolRegistry {
    pub fn new(adapter: Box<dyn DeviceAdapter>) -> Self {
        ToolRegistry {
            adapters: RwLock::new(vec![Arc::from(adapter)]),
        }
    }

    /// Attaches another adapter (e.g. a discovered peer). Duplicates are
    /// ignored by pointer identity.
    pub fn add_adapter(&self, adapter: Arc<dyn DeviceAdapter>) {
        if let Ok(mut adapters) = self.adapters.write() {
            if !adapters.iter().any(|existing| {
                std::ptr::eq(
                    existing.as_ref() as *const dyn DeviceAdapter,
                    adapter.as_ref() as *const dyn DeviceAdapter,
                )
            }) {
                adapters.push(adapter);
            }
        }
    }

    /// Detaches an adapter by pointer identity (used when a peer goes
    /// offline).
    pub fn remove_adapter(&self, adapter: &Arc<dyn DeviceAdapter>) {
        if let Ok(mut adapters) = self.adapters.write() {
            adapters.retain(|existing| {
                !std::ptr::eq(
                    existing.as_ref() as *const dyn DeviceAdapter,
                    adapter.as_ref() as *const dyn DeviceAdapter,
                )
            });
        }
    }

    pub fn all(&self) -> Vec<ToolDescriptor> {
        let mut tools = Vec::new();
        if let Ok(adapters) = self.adapters.read() {
            for adapter in adapters.iter() {
                tools.extend(adapter.tools());
            }
        }
        tools
    }

    /// Tools contributed by local adapters only. Used by the peer plane so a
    /// sibling's request is answered from this node alone.
    pub fn all_local(&self) -> Vec<ToolDescriptor> {
        let mut tools = Vec::new();
        if let Ok(adapters) = self.adapters.read() {
            for adapter in adapters.iter() {
                if adapter.is_peer() {
                    continue;
                }
                tools.extend(adapter.tools());
            }
        }
        tools
    }

    /// Descriptors visible to a caller at the given exposure level.
    pub fn visible(&self, exposure: &str) -> Vec<ToolDescriptor> {
        self.all()
            .into_iter()
            .filter(|tool| exposure_allows(exposure, &tool.exposure))
            .collect()
    }

    /// Local descriptors visible at the given exposure level.
    pub fn local_visible(&self, exposure: &str) -> Vec<ToolDescriptor> {
        self.all_local()
            .into_iter()
            .filter(|tool| exposure_allows(exposure, &tool.exposure))
            .collect()
    }

    /// Remote execution (HTTP control API / MCP): enforces both the exposure
    /// level and the tool's `remote_allowed` flag before touching the adapter.
    pub fn call_remote(
        &self,
        name: &str,
        arguments: Value,
        exposure: &str,
    ) -> Result<ToolResult, ToolError> {
        let adapter = self
            .adapter_for(name)
            .ok_or_else(|| ToolError::Unavailable(name.to_owned()))?;
        let tool = adapter
            .tools()
            .into_iter()
            .find(|tool| tool.name == name)
            .ok_or_else(|| ToolError::Unavailable(name.to_owned()))?;
        if !tool.remote_allowed {
            return Err(ToolError::NotAllowed(format!(
                "{name} may only run locally"
            )));
        }
        if !exposure_allows(exposure, &tool.exposure) {
            return Err(ToolError::NotAllowed(format!(
                "{name} requires a higher exposure level than {exposure}"
            )));
        }
        adapter.call(name, arguments)
    }

    /// In-process execution (agent loop, local orchestration): exposure still
    /// applies, but `remote_allowed` does not — the loop is the runtime itself.
    pub fn call_local(
        &self,
        name: &str,
        arguments: Value,
        exposure: &str,
    ) -> Result<ToolResult, ToolError> {
        let adapter = self
            .adapter_for(name)
            .ok_or_else(|| ToolError::Unavailable(name.to_owned()))?;
        let tool = adapter
            .tools()
            .into_iter()
            .find(|tool| tool.name == name)
            .ok_or_else(|| ToolError::Unavailable(name.to_owned()))?;
        if !exposure_allows(exposure, &tool.exposure) {
            return Err(ToolError::NotAllowed(format!(
                "{name} requires a higher exposure level than {exposure}"
            )));
        }
        adapter.call(name, arguments)
    }

    fn adapter_for(&self, name: &str) -> Option<Arc<dyn DeviceAdapter>> {
        let adapters = self.adapters.read().ok()?;
        adapters
            .iter()
            .find(|adapter| adapter.tools().iter().any(|tool| tool.name == name))
            .cloned()
    }
}

/// Exposure is cumulative: `action` sees everything, `sensor` sees read +
/// sensor tools, `read` sees read-only tools only.
fn exposure_allows(active: &str, tool: &str) -> bool {
    match active {
        "action" => true,
        "sensor" => tool == "read" || tool == "sensor",
        _ => tool == "read",
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use lociant_core::ToolResult;

    struct FakeDevice {
        tools: Vec<ToolDescriptor>,
    }

    impl DeviceAdapter for FakeDevice {
        fn tools(&self) -> Vec<ToolDescriptor> {
            self.tools.clone()
        }

        fn call(&self, name: &str, _arguments: Value) -> Result<ToolResult, ToolError> {
            Ok(ToolResult::ok(format!("ran {name}")))
        }
    }

    fn read_tool(name: &str) -> ToolDescriptor {
        ToolDescriptor {
            name: name.to_owned(),
            description: String::new(),
            arguments: Value::Null,
            exposure: "read".to_owned(),
            local: false,
            remote_allowed: true,
            requires_activity: false,
            side_effect: false,
            destructive: false,
            open_world: false,
        }
    }

    fn sensor_tool(name: &str) -> ToolDescriptor {
        let mut tool = read_tool(name);
        tool.exposure = "sensor".to_owned();
        tool
    }

    fn action_tool(name: &str) -> ToolDescriptor {
        let mut tool = read_tool(name);
        tool.exposure = "action".to_owned();
        tool.side_effect = true;
        tool
    }

    fn registry(tools: Vec<ToolDescriptor>) -> ToolRegistry {
        ToolRegistry::new(Box::new(FakeDevice { tools }))
    }

    struct FakePeer {
        tools: Vec<ToolDescriptor>,
    }

    impl DeviceAdapter for FakePeer {
        fn tools(&self) -> Vec<ToolDescriptor> {
            self.tools.clone()
        }

        fn call(&self, name: &str, _arguments: Value) -> Result<ToolResult, ToolError> {
            Ok(ToolResult::ok(format!("ran {name}")))
        }

        fn is_peer(&self) -> bool {
            true
        }
    }

    #[test]
    fn local_visible_excludes_peer_adapters() {
        let registry = registry(vec![read_tool("local_screen")]);
        registry.add_adapter(Arc::new(FakePeer {
            tools: vec![read_tool("peer_screen")],
        }));
        let names = |exposure: &str| -> Vec<String> {
            registry
                .local_visible(exposure)
                .into_iter()
                .map(|tool| tool.name)
                .collect()
        };
        assert_eq!(names("action"), vec!["local_screen".to_owned()]);
        assert_eq!(registry.visible("action").len(), 2);
    }

    #[test]
    fn exposure_filters_visible_tools() {
        let registry = registry(vec![
            read_tool("read_screen"),
            sensor_tool("read_sensor"),
            action_tool("tap"),
        ]);
        let names = |exposure: &str| -> Vec<String> {
            registry
                .visible(exposure)
                .into_iter()
                .map(|t| t.name)
                .collect()
        };
        assert_eq!(names("read"), vec!["read_screen"]);
        assert_eq!(names("sensor"), vec!["read_screen", "read_sensor"]);
        assert_eq!(names("action"), vec!["read_screen", "read_sensor", "tap"]);
    }

    #[test]
    fn remote_calls_enforce_policy() {
        let mut tap = action_tool("tap");
        tap.remote_allowed = false;
        let registry = registry(vec![read_tool("read_screen"), tap]);

        assert!(registry
            .call_remote("read_screen", Value::Null, "read")
            .is_ok());
        assert!(matches!(
            registry.call_remote("tap", Value::Null, "action"),
            Err(ToolError::NotAllowed(_))
        ));
        assert!(matches!(
            registry.call_remote("missing", Value::Null, "action"),
            Err(ToolError::Unavailable(_))
        ));
    }

    #[test]
    fn local_calls_skip_remote_allowed() {
        let mut tap = action_tool("tap");
        tap.remote_allowed = false;
        let registry = registry(vec![tap]);

        // The agent loop may run a local-only tool...
        assert!(registry.call_local("tap", Value::Null, "action").is_ok());
        // ...but remote callers still cannot.
        assert!(matches!(
            registry.call_remote("tap", Value::Null, "action"),
            Err(ToolError::NotAllowed(_))
        ));
    }
}
