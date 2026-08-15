//! Minimal Linux device adapter for the edge runtime.
//!
//! This is the first native desktop/edge tool set: filesystem and process
//! access are the lowest common denominator for a Linux edge node. Later
//! adapters (camera, GPIO, serial, MQTT) should follow the same pattern:
//! describe a tool, then implement a synchronous `DeviceAdapter` handler.

use std::process::Command;

use lociant_core::{ToolDescriptor, ToolResult};
use lociant_tools::{DeviceAdapter, ToolError};
use serde_json::{json, Value};

pub struct LinuxDevice;

trait ToolResultExt {
    fn with_structured(self, structured: Value) -> Self;
}

impl ToolResultExt for ToolResult {
    fn with_structured(mut self, structured: Value) -> Self {
        self.structured = structured;
        self
    }
}

impl DeviceAdapter for LinuxDevice {
    fn tools(&self) -> Vec<ToolDescriptor> {
        vec![
            tool(
                "device_status",
                "Return Linux edge node status: OS, architecture, hostname and local time.",
                json!({ "type": "object", "properties": {} }),
                "read",
                false,
                false,
            ),
            tool(
                "file_list",
                "List files and directories under a path.",
                json!({
                    "type": "object",
                    "properties": {
                        "path": { "type": "string", "description": "Directory path." }
                    },
                    "required": ["path"]
                }),
                "read",
                false,
                false,
            ),
            tool(
                "file_read",
                "Read a text file. Binary files are rejected.",
                json!({
                    "type": "object",
                    "properties": {
                        "path": { "type": "string", "description": "File path." },
                        "maxBytes": { "type": "integer", "description": "Maximum bytes to read. Default 65536." }
                    },
                    "required": ["path"]
                }),
                "read",
                false,
                false,
            ),
            tool(
                "file_write",
                "Write text to a file, creating parent directories when needed.",
                json!({
                    "type": "object",
                    "properties": {
                        "path": { "type": "string", "description": "File path." },
                        "content": { "type": "string", "description": "Text content to write." },
                        "append": { "type": "boolean", "description": "Append instead of overwrite. Default false." }
                    },
                    "required": ["path", "content"]
                }),
                "action",
                true,
                true,
            ),
            tool(
                "process_list",
                "List running processes (pid and command) using ps.",
                json!({ "type": "object", "properties": {} }),
                "read",
                false,
                false,
            ),
            tool(
                "process_run",
                "Run a shell command with a timeout and return stdout, stderr and exit code.",
                json!({
                    "type": "object",
                    "properties": {
                        "command": { "type": "string", "description": "Shell command to run." },
                        "timeoutSec": { "type": "integer", "description": "Timeout seconds. Default 30, max 300." }
                    },
                    "required": ["command"]
                }),
                "action",
                true,
                true,
            ),
        ]
    }

    fn call(&self, name: &str, arguments: Value) -> Result<ToolResult, ToolError> {
        match name {
            "device_status" => {
                Ok(ToolResult::ok("edge node status").with_structured(device_status()))
            }
            "file_list" => file_list(arguments),
            "file_read" => file_read(arguments),
            "file_write" => file_write(arguments),
            "process_list" => process_list(),
            "process_run" => process_run(arguments),
            _ => Err(ToolError::Unavailable(name.to_owned())),
        }
    }
}

fn tool(
    name: &str,
    description: &str,
    arguments: Value,
    exposure: &str,
    side_effect: bool,
    destructive: bool,
) -> ToolDescriptor {
    ToolDescriptor {
        name: name.to_owned(),
        description: description.to_owned(),
        arguments,
        exposure: exposure.to_owned(),
        local: true,
        remote_allowed: true,
        requires_activity: false,
        side_effect,
        destructive,
        open_world: side_effect,
    }
}

fn arg_str<'a>(args: &'a Value, key: &str) -> Result<&'a str, ToolError> {
    args.get(key)
        .and_then(Value::as_str)
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .ok_or_else(|| ToolError::Adapter(format!("{key} is required")))
}

fn arg_u64(args: &Value, key: &str, default: u64) -> u64 {
    args.get(key)
        .and_then(Value::as_u64)
        .filter(|v| *v > 0)
        .unwrap_or(default)
}

fn device_status() -> Value {
    let hostname = std::fs::read_to_string("/etc/hostname")
        .map(|s| s.trim().to_owned())
        .unwrap_or_default();
    json!({
        "os": std::env::consts::OS,
        "arch": std::env::consts::ARCH,
        "hostname": hostname,
        "unixTime": std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0),
    })
}

fn file_list(args: Value) -> Result<ToolResult, ToolError> {
    let path = arg_str(&args, "path")?;
    let entries = std::fs::read_dir(path)
        .map_err(|error| ToolError::Adapter(format!("read_dir {path}: {error}")))?;
    let mut out = Vec::new();
    for entry in entries.flatten() {
        let file_type = entry
            .file_type()
            .map(|ft| if ft.is_dir() { "dir" } else { "file" })
            .unwrap_or("unknown");
        out.push(json!({
            "name": entry.file_name().to_string_lossy(),
            "type": file_type,
        }));
    }
    out.sort_by(|a, b| {
        a.get("type")
            .and_then(Value::as_str)
            .cmp(&b.get("type").and_then(Value::as_str))
            .then_with(|| {
                a.get("name")
                    .and_then(Value::as_str)
                    .cmp(&b.get("name").and_then(Value::as_str))
            })
    });
    Ok(ToolResult::ok("directory listed").with_structured(json!({ "path": path, "entries": out })))
}

fn file_read(args: Value) -> Result<ToolResult, ToolError> {
    let path = arg_str(&args, "path")?;
    let max_bytes = arg_u64(&args, "maxBytes", 65536).min(1024 * 1024) as usize;
    let data =
        std::fs::read(path).map_err(|error| ToolError::Adapter(format!("read {path}: {error}")))?;
    if data.contains(&0) {
        return Err(ToolError::Adapter(format!(
            "{path} looks like a binary file"
        )));
    }
    let truncated = data.len() > max_bytes;
    let text =
        String::from_utf8_lossy(if truncated { &data[..max_bytes] } else { &data }).to_string();
    Ok(ToolResult::ok("file read").with_structured(json!({
        "path": path,
        "truncated": truncated,
        "size": data.len(),
        "content": text,
    })))
}

fn file_write(args: Value) -> Result<ToolResult, ToolError> {
    let path = arg_str(&args, "path")?;
    let content = arg_str(&args, "content")?;
    let append = args.get("append").and_then(Value::as_bool).unwrap_or(false);
    if let Some(parent) = std::path::Path::new(path).parent() {
        if !parent.as_os_str().is_empty() {
            std::fs::create_dir_all(parent).map_err(|error| {
                ToolError::Adapter(format!("mkdir {}: {error}", parent.display()))
            })?;
        }
    }
    if append {
        std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(path)
            .and_then(|mut file| {
                use std::io::Write;
                file.write_all(content.as_bytes())
            })
            .map_err(|error| ToolError::Adapter(format!("append {path}: {error}")))?;
    } else {
        std::fs::write(path, content)
            .map_err(|error| ToolError::Adapter(format!("write {path}: {error}")))?;
    }
    Ok(ToolResult::ok("file written")
        .with_structured(json!({ "path": path, "bytes": content.len() })))
}

fn process_list() -> Result<ToolResult, ToolError> {
    let output = Command::new("ps")
        .args(["-eo", "pid=,comm="])
        .output()
        .map_err(|error| ToolError::Adapter(format!("ps failed: {error}")))?;
    let text = String::from_utf8_lossy(&output.stdout);
    let processes = text
        .lines()
        .filter_map(|line| {
            let mut parts = line.splitn(2, char::is_whitespace);
            let pid = parts.next()?.trim().parse::<u32>().ok()?;
            let comm = parts.next()?.trim().to_owned();
            Some(json!({ "pid": pid, "command": comm }))
        })
        .collect::<Vec<_>>();
    Ok(ToolResult::ok("process list").with_structured(json!({ "processes": processes })))
}

fn process_run(args: Value) -> Result<ToolResult, ToolError> {
    let command = arg_str(&args, "command")?;
    let timeout_sec = arg_u64(&args, "timeoutSec", 30).min(300);
    let output = Command::new("timeout")
        .arg(timeout_sec.to_string())
        .arg("sh")
        .arg("-c")
        .arg(command)
        .output()
        .map_err(|error| ToolError::Adapter(format!("command spawn failed: {error}")))?;
    Ok(ToolResult::ok("command finished").with_structured(json!({
        "command": command,
        "exitCode": output.status.code(),
        "stdout": String::from_utf8_lossy(&output.stdout).to_string(),
        "stderr": String::from_utf8_lossy(&output.stderr).to_string(),
    })))
}
