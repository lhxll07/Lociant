//! First-class llama.cpp backend.
//!
//! Lociant spawns a local `llama-server` subprocess (the OpenAI-compatible
//! server shipped with llama.cpp) and exposes it as a regular local model.
//! On Android the same path can be used by bundling an ARM64 `llama-server`
//! binary and passing `LOCIANT_LLAMA_*` environment variables from the Kotlin
//! host; alternatively `llamaBaseUrl` points at an already-running server.

use std::process::{Child, Command, Stdio};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use anyhow::{anyhow, Context, Result};
use serde_json::Value;

pub struct LlamaServer {
    pub model: String,
    child: Mutex<Option<Child>>,
}

impl Drop for LlamaServer {
    fn drop(&mut self) {
        if let Ok(mut guard) = self.child.lock() {
            if let Some(mut child) = guard.take() {
                let _ = child.kill();
                let _ = child.wait();
            }
        }
    }
}

pub async fn start(settings: &Value) -> Result<Option<Arc<LlamaServer>>> {
    let base_url =
        setting_or_env(settings, "llamaBaseUrl", "LOCIANT_LLAMA_BASE_URL").unwrap_or_default();
    let model = setting_or_env(settings, "llamaModelName", "LOCIANT_LLAMA_MODEL_NAME")
        .unwrap_or_else(|| "llama".to_owned());
    let enabled = settings.get("llamaEnabled").and_then(Value::as_bool) == Some(true)
        || env_flag("LOCIANT_LLAMA_ENABLED")
        || !base_url.is_empty();

    if !enabled {
        return Ok(None);
    }

    if !base_url.is_empty() {
        return Ok(Some(Arc::new(LlamaServer {
            model,
            child: Mutex::new(None),
        })));
    }

    let model_path = setting_or_env(settings, "llamaModelPath", "LOCIANT_LLAMA_MODEL_PATH")
        .ok_or_else(|| {
            anyhow!("llamaEnabled requires llamaModelPath or LOCIANT_LLAMA_MODEL_PATH")
        })?;
    let server_path = setting_or_env(settings, "llamaServerPath", "LOCIANT_LLAMA_SERVER_PATH")
        .unwrap_or_else(|| "llama-server".to_owned());
    let port = match settings
        .get("llamaPort")
        .and_then(Value::as_u64)
        .map(|v| v as u16)
        .filter(|port| *port != 0)
        .or_else(|| {
            std::env::var("LOCIANT_LLAMA_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .filter(|port| *port != 0)
        }) {
        Some(port) => port,
        None => free_port()?,
    };
    let threads = setting_or_env(settings, "llamaThreads", "LOCIANT_LLAMA_THREADS")
        .unwrap_or_else(|| "8".to_owned());
    let ctx_size = setting_or_env(settings, "llamaCtxSize", "LOCIANT_LLAMA_CTX_SIZE")
        .unwrap_or_else(|| "4096".to_owned());
    let n_predict = setting_or_env(settings, "llamaPredict", "LOCIANT_LLAMA_PREDICT")
        .unwrap_or_else(|| "512".to_owned());
    let lib_dir = setting_or_env(settings, "llamaLibDir", "LOCIANT_LLAMA_LIB_DIR");

    let mut command = Command::new(&server_path);
    if let Some(lib_dir) = &lib_dir {
        command.env("LD_LIBRARY_PATH", lib_dir);
    }
    let child = command
        .arg("-m")
        .arg(&model_path)
        .arg("--port")
        .arg(port.to_string())
        .arg("--host")
        .arg("127.0.0.1")
        .arg("--alias")
        .arg(&model)
        .arg("--ctx-size")
        .arg(ctx_size)
        .arg("--n-predict")
        .arg(n_predict)
        .arg("--threads")
        .arg(threads)
        .stdout(Stdio::null())
        .stderr(Stdio::inherit())
        .spawn()
        .with_context(|| format!("spawn llama-server {server_path}"))?;

    let server = Arc::new(LlamaServer {
        model,
        child: Mutex::new(Some(child)),
    });

    let health_url = format!("http://127.0.0.1:{port}/health");
    for _ in 0..80 {
        if reqwest::get(&health_url)
            .await
            .is_ok_and(|response| response.status().is_success())
        {
            tracing::info!("llama-server ready on port {port}");
            return Ok(Some(server));
        }
        tokio::time::sleep(Duration::from_millis(250)).await;
    }
    Err(anyhow!(
        "llama-server did not become healthy on port {port}"
    ))
}

fn setting_or_env(settings: &Value, key: &str, env: &str) -> Option<String> {
    settings
        .get(key)
        .and_then(Value::as_str)
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(str::to_owned)
        .or_else(|| std::env::var(env).ok().filter(|s| !s.trim().is_empty()))
}

fn env_flag(name: &str) -> bool {
    std::env::var(name)
        .ok()
        .map(|value| {
            matches!(
                value.trim().to_ascii_lowercase().as_str(),
                "1" | "true" | "yes" | "on"
            )
        })
        .unwrap_or(false)
}

fn free_port() -> Result<u16> {
    let listener = std::net::TcpListener::bind("127.0.0.1:0")?;
    listener
        .local_addr()
        .map(|addr| addr.port())
        .context("pick free port")
}
