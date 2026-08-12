//! Optional SlumberGuard integration behind the `slumberguard` feature.

use std::sync::Arc;
#[cfg(feature = "slumberguard")]
use std::sync::Mutex;
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
    runtime: Arc<Mutex<MonitorRuntime<FfmpegMotionSource, MicEnergySource, NoopSemanticJudge>>>,
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
        let service = Arc::new(Self {
            runtime: Arc::new(Mutex::new(runtime)),
        });
        let runtime = service.runtime.clone();
        std::thread::spawn(move || loop {
            if let Ok(mut runtime) = runtime.lock() {
                let _ = runtime.step_once();
            }
            std::thread::sleep(Duration::from_millis(500));
        });
        tracing::info!("baby monitor started (camera: {camera_device})");
        service
    }

    fn snapshot_value(&self) -> Value {
        let runtime = self.runtime.lock().expect("baby runtime lock");
        let (state, latest, events) =
            (runtime.state(), runtime.latest(), runtime.recent_events(20));
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
