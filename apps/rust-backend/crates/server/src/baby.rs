//! Optional SlumberGuard integration behind the `slumberguard` feature.

use std::sync::Arc;
#[cfg(feature = "slumberguard")]
use std::sync::RwLock;
#[cfg(feature = "slumberguard")]
use std::time::Duration;

#[cfg(feature = "slumberguard")]
use serde_json::json;
use serde_json::Value;
/// Narrow capability boundary used by the server and peer API.
pub trait BabyMonitor: Send + Sync {
    fn snapshot(&self) -> Value;
}

#[cfg(feature = "slumberguard")]
use slumberguard_core::{BabyStateEngine, EngineConfig};
#[cfg(feature = "slumberguard")]
use slumberguard_runtime::{MonitorConfig, MonitorRuntime};
#[cfg(feature = "slumberguard")]
use slumberguard_source::linux::{FfmpegMotionSource, MicEnergySource, NoopSemanticJudge};

#[cfg(feature = "slumberguard")]
pub struct BabyService {
    snapshot: Arc<RwLock<Value>>,
}

#[cfg(feature = "slumberguard")]
impl BabyService {
    fn start_service(camera_device: &str, mic_device: &str) -> Arc<dyn BabyMonitor> {
        let visual = FfmpegMotionSource::new(camera_device);
        let audio = MicEnergySource::new(mic_device);
        let runtime = MonitorRuntime::new(
            visual,
            audio,
            NoopSemanticJudge,
            BabyStateEngine::new(EngineConfig::default()),
            MonitorConfig::default(),
        );
        let snapshot = Arc::new(RwLock::new(json!({
            "state": "Idle",
            "latest": Value::Null,
            "audioError": Value::Null,
            "events": [],
        })));
        let service = Arc::new(Self {
            snapshot: snapshot.clone(),
        });
        std::thread::spawn(move || {
            let mut runtime = runtime;
            loop {
                let _ = runtime.step_once();
                let value = snapshot_value(&runtime);
                if let Ok(mut current) = snapshot.write() {
                    *current = value;
                }
                std::thread::sleep(Duration::from_millis(500));
            }
        });
        tracing::info!("baby monitor started (camera: {camera_device})");
        service
    }

    fn snapshot_value(&self) -> Value {
        self.snapshot
            .read()
            .map(|snapshot| snapshot.clone())
            .unwrap_or_else(|_| json!({ "error": "baby monitor unavailable" }))
    }
}

#[cfg(feature = "slumberguard")]
fn snapshot_value(
    runtime: &MonitorRuntime<FfmpegMotionSource, MicEnergySource, NoopSemanticJudge>,
) -> Value {
    let (state, latest, events) = (runtime.state(), runtime.latest(), runtime.recent_events(20));
    let latest_json = latest.map(|event| {
        json!({
            "state": format!("{state:?}"),
            "action": format!("{:?}", event.decision.action),
            "reason": event.decision.reason,
            "motion": event.evidence.visual.motion_strength,
            "audio": format!("{:?}", event.evidence.audio),
        })
    });
    let audio_error = runtime.audio_error();
    json!({
        "state": format!("{state:?}"),
        "latest": latest_json,
        "audioError": audio_error,
        "events": events.iter().map(|event| json!({
            "state": format!("{:?}", event.decision.state),
            "action": format!("{:?}", event.decision.action),
            "reason": event.decision.reason,
            "motion": event.evidence.visual.motion_strength,
            "audio": format!("{:?}", event.evidence.audio),
            "timestamp": event.evidence.timestamp,
        })).collect::<Vec<_>>(),
    })
}

#[cfg(feature = "slumberguard")]
impl BabyMonitor for BabyService {
    fn snapshot(&self) -> Value {
        self.snapshot_value()
    }
}

#[cfg(feature = "slumberguard")]
pub fn start(camera_device: &str, mic_device: &str) -> Option<Arc<dyn BabyMonitor>> {
    Some(BabyService::start_service(camera_device, mic_device))
}

#[cfg(not(feature = "slumberguard"))]
pub fn start(_camera_device: &str, _mic_device: &str) -> Option<Arc<dyn BabyMonitor>> {
    tracing::warn!("SlumberGuard support is not compiled in; baby monitor disabled");
    None
}

#[cfg(not(feature = "slumberguard"))]
#[test]
fn disabled_feature_does_not_start_monitor() {
    assert!(start("/dev/video0", "default").is_none());
}
