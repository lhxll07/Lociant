//! 眠安智护（SlumberGuard）集成：运行婴儿监控采样，暴露状态 API。

use std::sync::{Arc, Mutex};
use std::time::Duration;

use serde_json::{json, Value};
use slumberguard_core::{BabyStateEngine, EngineConfig};
use slumberguard_runtime::{MonitorConfig, MonitorRuntime};
use slumberguard_source::linux::{FfmpegMotionSource, MicEnergySource, NoopSemanticJudge};

pub struct BabyService {
    runtime: Arc<Mutex<MonitorRuntime<FfmpegMotionSource, MicEnergySource, NoopSemanticJudge>>>,
}

impl BabyService {
    pub fn start(camera_device: &str, mic_device: &str) -> Arc<Self> {
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

    pub fn snapshot(&self) -> Value {
        let runtime = self.runtime.lock().expect("baby runtime lock");
        let (state, latest, events) = (
            runtime.state(),
            runtime.latest(),
            runtime.recent_events(20),
        );
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
