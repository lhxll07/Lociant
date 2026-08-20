//! First-class llama.cpp backend.
//!
//! Lociant spawns a local `llama-server` subprocess (the OpenAI-compatible
//! server shipped with llama.cpp) and exposes it as a regular local model.
//! On Android the same path can be used by bundling an ARM64 `llama-server`
//! binary and passing `LOCIANT_LLAMA_*` environment variables from the Kotlin
//! host; alternatively `llamaBaseUrl` points at an already-running server.

use std::process::{Child, Command, Stdio};
use std::sync::{
    atomic::{AtomicBool, Ordering},
    Arc, Mutex, Weak,
};
use std::time::Duration;

use anyhow::{anyhow, Context, Result};
use serde_json::Value;

const HEALTH_CHECK_TIMEOUT: Duration = Duration::from_secs(3);
const HEALTH_POLL_INTERVAL: Duration = Duration::from_secs(2);
const STARTUP_ATTEMPTS: usize = 80;

#[derive(Clone)]
struct LaunchConfig {
    server_path: String,
    model_path: String,
    model: String,
    port: u16,
    threads: String,
    ctx_size: String,
    n_predict: String,
    lib_dir: Option<String>,
}

pub struct LlamaServer {
    pub model: String,
    health_url: String,
    child: Mutex<Option<Child>>,
    launch: Option<LaunchConfig>,
    healthy: AtomicBool,
    restarting: AtomicBool,
    stopping: AtomicBool,
}

impl Drop for LlamaServer {
    fn drop(&mut self) {
        self.stopping.store(true, Ordering::Release);
        self.healthy.store(false, Ordering::Release);
        self.terminate_child();
    }
}

impl LlamaServer {
    /// Returns the state of the private llama.cpp process for the runtime API.
    /// An external base URL is considered ready because Lociant does not own
    /// its lifecycle.
    pub fn status(&self) -> &'static str {
        if self.restarting.load(Ordering::Acquire) {
            "restarting"
        } else if self.healthy.load(Ordering::Acquire) {
            "ready"
        } else {
            "offline"
        }
    }

    pub fn is_ready(&self) -> bool {
        self.healthy.load(Ordering::Acquire)
    }

    fn spawn_child(&self) -> Result<()> {
        let launch = self
            .launch
            .as_ref()
            .ok_or_else(|| anyhow!("llama-server is externally managed"))?;
        if self.stopping.load(Ordering::Acquire) {
            return Err(anyhow!("llama-server supervisor is stopping"));
        }

        let child = spawn_command(launch)?;
        let mut guard = self.child.lock().expect("llama child lock");
        if self.stopping.load(Ordering::Acquire) {
            drop(guard);
            terminate(child);
            return Err(anyhow!("llama-server supervisor is stopping"));
        }
        if guard.is_some() {
            drop(guard);
            terminate(child);
            return Err(anyhow!("llama-server child is already running"));
        }
        *guard = Some(child);
        Ok(())
    }

    fn child_exited(&self) -> bool {
        let mut guard = self.child.lock().expect("llama child lock");
        let outcome = match guard.as_mut() {
            Some(child) => match child.try_wait() {
                Ok(Some(status)) => Some(Ok(status)),
                Ok(None) => None,
                Err(error) => Some(Err(error)),
            },
            None => None,
        };
        let Some(outcome) = outcome else {
            return false;
        };
        guard.take();
        self.healthy.store(false, Ordering::Release);
        self.restarting.store(true, Ordering::Release);
        match outcome {
            Ok(status) => tracing::warn!(?status, "llama-server exited"),
            Err(error) => tracing::warn!(%error, "llama-server status check failed"),
        }
        true
    }

    fn terminate_child(&self) {
        let child = self.child.lock().ok().and_then(|mut guard| guard.take());
        if let Some(child) = child {
            terminate(child);
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
            health_url: format!("{}/health", base_url.trim_end_matches('/')),
            child: Mutex::new(None),
            launch: None,
            healthy: AtomicBool::new(true),
            restarting: AtomicBool::new(false),
            stopping: AtomicBool::new(false),
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

    let launch = LaunchConfig {
        server_path,
        model_path,
        model: model.clone(),
        port,
        threads,
        ctx_size,
        n_predict,
        lib_dir,
    };
    let server = Arc::new(LlamaServer {
        model: model.clone(),
        health_url: format!("http://127.0.0.1:{port}/health"),
        child: Mutex::new(None),
        launch: Some(launch),
        healthy: AtomicBool::new(false),
        restarting: AtomicBool::new(false),
        stopping: AtomicBool::new(false),
    });

    server.spawn_child()?;
    if !wait_until_healthy(&server).await {
        server.stopping.store(true, Ordering::Release);
        server.terminate_child();
        return Err(anyhow!(
            "llama-server did not become healthy on port {port}"
        ));
    }
    tracing::info!("llama-server ready on port {port}");
    spawn_supervisor(&server);
    Ok(Some(server))
}

fn spawn_command(launch: &LaunchConfig) -> Result<Child> {
    let mut command = Command::new(&launch.server_path);
    if let Some(lib_dir) = &launch.lib_dir {
        command.env("LD_LIBRARY_PATH", lib_dir);
    }
    command
        .arg("-m")
        .arg(&launch.model_path)
        .arg("--port")
        .arg(launch.port.to_string())
        .arg("--host")
        .arg("127.0.0.1")
        .arg("--alias")
        .arg(&launch.model)
        .arg("--ctx-size")
        .arg(&launch.ctx_size)
        .arg("--n-predict")
        .arg(&launch.n_predict)
        .arg("--threads")
        .arg(&launch.threads)
        .stdout(Stdio::null())
        .stderr(Stdio::inherit())
        .spawn()
        .with_context(|| format!("spawn llama-server {}", launch.server_path))
}

fn terminate(mut child: Child) {
    let _ = child.kill();
    let _ = child.wait();
}

async fn wait_until_healthy(server: &LlamaServer) -> bool {
    for _ in 0..STARTUP_ATTEMPTS {
        if server.child_exited() {
            return false;
        }
        if check_health(&server.health_url).await {
            server.healthy.store(true, Ordering::Release);
            server.restarting.store(false, Ordering::Release);
            return true;
        }
        tokio::time::sleep(Duration::from_millis(250)).await;
    }
    false
}

fn spawn_supervisor(server: &Arc<LlamaServer>) {
    let weak = Arc::downgrade(server);
    tokio::spawn(async move { supervise(weak).await });
}

async fn supervise(weak: Weak<LlamaServer>) {
    loop {
        tokio::time::sleep(HEALTH_POLL_INTERVAL).await;
        let Some(server) = weak.upgrade() else {
            return;
        };
        if server.stopping.load(Ordering::Acquire) {
            return;
        }

        let exited = server.child_exited();
        if !exited && check_health(&server.health_url).await {
            server.healthy.store(true, Ordering::Release);
            server.restarting.store(false, Ordering::Release);
            continue;
        }

        server.healthy.store(false, Ordering::Release);
        server.restarting.store(true, Ordering::Release);
        server.terminate_child();
        drop(server);

        let mut attempt = 0;
        loop {
            let Some(server) = weak.upgrade() else {
                return;
            };
            if server.stopping.load(Ordering::Acquire) {
                return;
            }
            let delay = restart_delay(attempt);
            tracing::warn!(?delay, attempt, "restarting llama-server");
            drop(server);
            tokio::time::sleep(delay).await;
            let Some(server) = weak.upgrade() else {
                return;
            };
            if server.stopping.load(Ordering::Acquire) {
                return;
            }

            let recovered = match server.spawn_child() {
                Ok(()) => wait_until_healthy(&server).await,
                Err(error) => {
                    tracing::warn!(%error, attempt, "llama-server restart failed");
                    false
                }
            };
            if recovered {
                tracing::info!(attempt, "llama-server recovered");
                server.restarting.store(false, Ordering::Release);
                break;
            }
            server.terminate_child();
            server.healthy.store(false, Ordering::Release);
            attempt = attempt.saturating_add(1);
        }
    }
}

async fn check_health(url: &str) -> bool {
    reqwest::Client::new()
        .get(url)
        .timeout(HEALTH_CHECK_TIMEOUT)
        .send()
        .await
        .is_ok_and(|response| response.status().is_success())
}

fn restart_delay(attempt: usize) -> Duration {
    Duration::from_secs(1u64 << attempt.min(4))
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

#[cfg(test)]
mod tests {
    use super::restart_delay;
    use std::time::Duration;

    #[test]
    fn restart_delay_is_bounded() {
        assert_eq!(restart_delay(0), Duration::from_secs(1));
        assert_eq!(restart_delay(3), Duration::from_secs(8));
        assert_eq!(restart_delay(99), Duration::from_secs(16));
    }
}
