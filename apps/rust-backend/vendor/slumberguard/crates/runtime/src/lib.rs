//! 采样运行时：定时聚合视觉/音频/语义证据 -> 状态机 -> 事件缓存。

use std::collections::VecDeque;
use std::time::Duration;

use slumberguard_core::{BabyState, BabyStateEngine, Decision, Evidence};
use slumberguard_source::{AudioSource, SemanticJudge, VisualSource};

#[derive(Debug, Clone)]
pub struct MonitorConfig {
    pub interval: Duration,
    pub max_events: usize,
}

impl Default for MonitorConfig {
    fn default() -> Self {
        Self {
            interval: Duration::from_millis(500),
            max_events: 100,
        }
    }
}

#[derive(Debug, Clone)]
pub struct MonitorEvent {
    pub decision: Decision,
    pub evidence: Evidence,
}

/// 多源采样运行时：每次采样读取三个模块并合并为 Evidence。
pub struct MonitorRuntime<V: VisualSource, A: AudioSource, J: SemanticJudge> {
    visual: V,
    audio: A,
    semantic: J,
    engine: BabyStateEngine,
    config: MonitorConfig,
    events: VecDeque<MonitorEvent>,
    latest: Option<MonitorEvent>,
}

impl<V: VisualSource, A: AudioSource, J: SemanticJudge> MonitorRuntime<V, A, J> {
    pub fn new(
        visual: V,
        audio: A,
        semantic: J,
        engine: BabyStateEngine,
        config: MonitorConfig,
    ) -> Self {
        let max_events = config.max_events;
        Self {
            visual,
            audio,
            semantic,
            engine,
            config,
            events: VecDeque::with_capacity(max_events),
            latest: None,
        }
    }

    /// 执行一次采样：读三个证据源 -> 合并 Evidence -> 状态机。
    pub fn step_once(&mut self) -> anyhow::Result<Decision> {
        let timestamp = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs_f64())
            .unwrap_or(0.0);
        let evidence = Evidence {
            timestamp,
            visual: self.visual.read_visual()?,
            audio: self.audio.read_audio()?,
            semantic: self.semantic.judge(timestamp),
        };
        let decision = self.engine.step(&evidence);
        let event = MonitorEvent {
            decision: decision.clone(),
            evidence,
        };
        if self.events.len() >= self.config.max_events {
            self.events.pop_front();
        }
        self.events.push_back(event.clone());
        self.latest = Some(event);
        Ok(decision)
    }

    pub fn interval(&self) -> Duration {
        self.config.interval
    }

    pub fn state(&self) -> BabyState {
        self.engine.state()
    }

    /// 音频源最近的采集错误（供监控 API 展示）。
    pub fn audio_error(&self) -> Option<String> {
        self.audio.last_error()
    }

    pub fn latest(&self) -> Option<&MonitorEvent> {
        self.latest.as_ref()
    }

    pub fn recent_events(&self, limit: usize) -> Vec<&MonitorEvent> {
        self.events.iter().rev().take(limit).collect()
    }

    /// 后台循环：持续采样（由调用方驱动线程）。
    pub fn run_forever(&mut self) -> anyhow::Result<()> {
        loop {
            let _ = self.step_once();
            std::thread::sleep(self.config.interval);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use slumberguard_core::{AudioCandidate, EngineConfig, VisualCandidate};
    use slumberguard_source::linux::NoopSemanticJudge;

    struct FakeVisual;
    impl VisualSource for FakeVisual {
        fn read_visual(&mut self) -> anyhow::Result<VisualCandidate> {
            Ok(VisualCandidate::default())
        }
    }

    struct FakeAudio;
    impl AudioSource for FakeAudio {
        fn read_audio(&mut self) -> anyhow::Result<AudioCandidate> {
            Ok(AudioCandidate::Silent)
        }
    }

    #[test]
    fn step_merges_sources_and_records_events() {
        let mut runtime = MonitorRuntime::new(
            FakeVisual,
            FakeAudio,
            NoopSemanticJudge,
            BabyStateEngine::new(EngineConfig::default()),
            MonitorConfig::default(),
        );
        runtime.step_once().unwrap();
        assert_eq!(runtime.state(), BabyState::Idle);
        assert_eq!(runtime.recent_events(10).len(), 1);
    }
}
