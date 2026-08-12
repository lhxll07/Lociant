//! SlumberGuard 状态机核心（跨平台纯逻辑）。
//!
//! 忠于大创项目《婴幼儿夜间哭闹前波动状态辅助安抚系统》：
//! 视觉候选识别 + 关键帧语义标签 + 音频候选识别 -> 观察窗口 -> 融合确认
//! -> 一级/二级声学安抚 -> 家长提示 -> 冷却返回。

/// 系统状态（对应申请书主流程）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BabyState {
    /// 观察待机
    Idle,
    /// 候选观察（等待窗口内确认）
    Candidate,
    /// 一级声学安抚（粉红/棕色噪音等遮蔽型低刺激声音）
    Soothing1,
    /// 二级声学安抚（类宫内声等组合）
    Soothing2,
    /// 家长提示（持续哭声/安抚无效/成人入画等）
    NotifyParent,
    /// 冷却返回
    Cooldown,
}

/// 视觉候选事件：帧间运动强度与持续时间（由视觉候选识别模块产生）。
#[derive(Debug, Clone, Copy, Default)]
pub struct VisualCandidate {
    pub motion_strength: f64,
    pub duration_ticks: u32,
    pub has_adult: bool,
}

/// 关键帧语义标签（由边缘侧量化视觉语言模型输出）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SemanticLabel {
    NormalSleep,
    TurnOver,
    SelfSoothing,
    MildFuss,
    AdultInterference,
    Background,
}

/// 音频候选事件（短时能量/频段能量等基础特征）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AudioCandidate {
    Silent,
    Hum,
    CryLike,
    Noise,
}

/// 一次采样的多模态证据。
#[derive(Debug, Clone)]
pub struct Evidence {
    pub timestamp: f64,
    pub visual: VisualCandidate,
    pub semantic: Option<SemanticLabel>,
    pub audio: AudioCandidate,
}

impl Evidence {
    pub fn new(timestamp: f64) -> Self {
        Self {
            timestamp,
            visual: VisualCandidate::default(),
            semantic: None,
            audio: AudioCandidate::Silent,
        }
    }

    /// 是否存在需要关注的活动信号（视觉运动或哭声样音频）。
    pub fn active(&self) -> bool {
        self.visual.motion_strength > 0.0 || self.audio == AudioCandidate::CryLike
    }
}

/// 状态机动作（对应申请书“忽略/观察/一级/二级/提示家长”）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Action {
    Ignore,
    Observe,
    Sooth1,
    Sooth2,
    NotifyParent,
}

#[derive(Debug, Clone)]
pub struct Decision {
    pub previous_state: BabyState,
    pub state: BabyState,
    pub action: Action,
    pub reason: String,
    pub should_notify: bool,
}

#[derive(Debug, Clone, Copy)]
pub struct EngineConfig {
    /// 视觉运动强度阈值（0-1）。
    pub motion_threshold: f64,
    /// 候选观察窗口（采样次数）。
    pub observe_ticks: u32,
    /// 一级安抚窗口。
    pub sooth1_ticks: u32,
    /// 二级安抚窗口。
    pub sooth2_ticks: u32,
    /// 冷却窗口。
    pub cooldown_ticks: u32,
}

impl Default for EngineConfig {
    fn default() -> Self {
        Self {
            motion_threshold: 0.1,
            observe_ticks: 3,
            sooth1_ticks: 4,
            sooth2_ticks: 4,
            cooldown_ticks: 5,
        }
    }
}

/// 多模态融合状态机。
pub struct BabyStateEngine {
    config: EngineConfig,
    state: BabyState,
    window_ticks: u32,
    cooldown_ticks: u32,
}

impl BabyStateEngine {
    pub fn new(config: EngineConfig) -> Self {
        Self {
            config,
            state: BabyState::Idle,
            window_ticks: 0,
            cooldown_ticks: 0,
        }
    }

    pub fn state(&self) -> BabyState {
        self.state
    }

    pub fn step(&mut self, evidence: &Evidence) -> Decision {
        let previous = self.state;
        let motion = evidence.visual.motion_strength >= self.config.motion_threshold;
        let crying = evidence.audio == AudioCandidate::CryLike;
        let adult = evidence.visual.has_adult
            || evidence.semantic == Some(SemanticLabel::AdultInterference);

        match self.state {
            BabyState::Idle => {
                if crying || adult {
                    // 哭声样或成人干扰：直接进入家长提示边界（融合规则之一）。
                    self.enter(BabyState::NotifyParent, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::NotifyParent,
                        "cry_or_adult_from_idle",
                        true,
                    )
                } else if motion || evidence.audio == AudioCandidate::Hum {
                    self.enter(BabyState::Candidate, 1);
                    Decision::new(
                        previous,
                        self.state,
                        Action::Observe,
                        "candidate_started",
                        false,
                    )
                } else {
                    Decision::new(previous, self.state, Action::Ignore, "no_activity", false)
                }
            }
            BabyState::Candidate => {
                if crying || adult {
                    self.enter(BabyState::NotifyParent, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::NotifyParent,
                        "cry_or_adult_during_observe",
                        true,
                    )
                } else if motion {
                    self.window_ticks += 1;
                    if self.window_ticks >= self.config.observe_ticks {
                        self.enter(BabyState::Soothing1, 1);
                        Decision::new(
                            previous,
                            self.state,
                            Action::Sooth1,
                            "activity_confirmed",
                            false,
                        )
                    } else {
                        Decision::new(
                            previous,
                            self.state,
                            Action::Observe,
                            "waiting_for_confirmation",
                            false,
                        )
                    }
                } else {
                    self.enter(BabyState::Idle, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::Ignore,
                        "candidate_dropped",
                        false,
                    )
                }
            }
            BabyState::Soothing1 => {
                if crying || adult {
                    self.enter(BabyState::NotifyParent, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::NotifyParent,
                        "cry_or_adult_during_sooth1",
                        true,
                    )
                } else if motion {
                    self.window_ticks += 1;
                    if self.window_ticks >= self.config.sooth1_ticks {
                        self.enter(BabyState::Soothing2, 1);
                        Decision::new(
                            previous,
                            self.state,
                            Action::Sooth2,
                            "sooth1_ineffective",
                            false,
                        )
                    } else {
                        Decision::new(
                            previous,
                            self.state,
                            Action::Sooth1,
                            "soothing1_playing",
                            false,
                        )
                    }
                } else {
                    self.enter(BabyState::Cooldown, self.config.cooldown_ticks);
                    Decision::new(
                        previous,
                        self.state,
                        Action::Ignore,
                        "sooth1_settled",
                        false,
                    )
                }
            }
            BabyState::Soothing2 => {
                if crying || adult {
                    self.enter(BabyState::NotifyParent, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::NotifyParent,
                        "cry_or_adult_during_sooth2",
                        true,
                    )
                } else if motion {
                    self.window_ticks += 1;
                    if self.window_ticks >= self.config.sooth2_ticks {
                        self.enter(BabyState::NotifyParent, 0);
                        Decision::new(
                            previous,
                            self.state,
                            Action::NotifyParent,
                            "sooth2_ineffective",
                            true,
                        )
                    } else {
                        Decision::new(
                            previous,
                            self.state,
                            Action::Sooth2,
                            "soothing2_playing",
                            false,
                        )
                    }
                } else {
                    self.enter(BabyState::Cooldown, self.config.cooldown_ticks);
                    Decision::new(
                        previous,
                        self.state,
                        Action::Ignore,
                        "sooth2_settled",
                        false,
                    )
                }
            }
            BabyState::NotifyParent => {
                self.enter(BabyState::Cooldown, self.config.cooldown_ticks);
                Decision::new(
                    previous,
                    self.state,
                    Action::Ignore,
                    "parent_notified",
                    true,
                )
            }
            BabyState::Cooldown => {
                if crying {
                    self.enter(BabyState::NotifyParent, 0);
                    Decision::new(
                        previous,
                        self.state,
                        Action::NotifyParent,
                        "cry_during_cooldown",
                        true,
                    )
                } else {
                    self.cooldown_ticks = self.cooldown_ticks.saturating_sub(1);
                    if self.cooldown_ticks == 0 {
                        self.enter(BabyState::Idle, 0);
                        Decision::new(
                            previous,
                            self.state,
                            Action::Ignore,
                            "cooldown_finished",
                            false,
                        )
                    } else {
                        Decision::new(
                            previous,
                            self.state,
                            Action::Ignore,
                            "cooldown_active",
                            false,
                        )
                    }
                }
            }
        }
    }

    fn enter(&mut self, state: BabyState, window_ticks: u32) {
        self.state = state;
        self.window_ticks = window_ticks;
    }
}

impl Decision {
    fn new(
        previous_state: BabyState,
        state: BabyState,
        action: Action,
        reason: &str,
        should_notify: bool,
    ) -> Self {
        Self {
            previous_state,
            state,
            action,
            reason: reason.to_owned(),
            should_notify,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn engine() -> BabyStateEngine {
        BabyStateEngine::new(EngineConfig {
            observe_ticks: 3,
            sooth1_ticks: 3,
            sooth2_ticks: 3,
            cooldown_ticks: 2,
            ..EngineConfig::default()
        })
    }

    fn evidence(timestamp: f64, motion: f64, audio: AudioCandidate) -> Evidence {
        Evidence {
            timestamp,
            visual: VisualCandidate {
                motion_strength: motion,
                duration_ticks: 0,
                has_adult: false,
            },
            semantic: None,
            audio,
        }
    }

    #[test]
    fn normal_sleep_is_ignored() {
        let mut engine = engine();
        let d = engine.step(&evidence(1.0, 0.0, AudioCandidate::Silent));
        assert_eq!(d.state, BabyState::Idle);
        assert_eq!(d.action, Action::Ignore);
    }

    #[test]
    fn sustained_fuss_escalates_to_soothing_and_notify() {
        let mut engine = engine();
        // 观察窗口：3 次持续运动 -> 一级安抚
        engine.step(&evidence(1.0, 0.8, AudioCandidate::Silent));
        engine.step(&evidence(2.0, 0.8, AudioCandidate::Silent));
        let d = engine.step(&evidence(3.0, 0.8, AudioCandidate::Silent));
        assert_eq!(d.state, BabyState::Soothing1);
        assert_eq!(d.action, Action::Sooth1);

        // 一级安抚 2 次仍运动 -> 二级安抚
        engine.step(&evidence(4.0, 0.8, AudioCandidate::Silent));
        let d = engine.step(&evidence(5.0, 0.8, AudioCandidate::Silent));
        assert_eq!(d.state, BabyState::Soothing2);
        assert_eq!(d.action, Action::Sooth2);

        // 二级安抚 2 次仍运动 -> 家长提示
        engine.step(&evidence(6.0, 0.8, AudioCandidate::Silent));
        let d = engine.step(&evidence(7.0, 0.8, AudioCandidate::Silent));
        assert_eq!(d.state, BabyState::NotifyParent);
        assert!(d.should_notify);
    }

    #[test]
    fn cry_immediately_notifies() {
        let mut engine = engine();
        let d = engine.step(&evidence(1.0, 0.0, AudioCandidate::CryLike));
        assert_eq!(d.state, BabyState::NotifyParent);
        assert!(d.should_notify);
    }

    #[test]
    fn soothing_settles_then_cooldown_returns_idle() {
        let mut engine = engine();
        engine.step(&evidence(1.0, 0.8, AudioCandidate::Silent));
        engine.step(&evidence(2.0, 0.8, AudioCandidate::Silent));
        engine.step(&evidence(3.0, 0.8, AudioCandidate::Silent)); // -> Soothing1
        let d = engine.step(&evidence(4.0, 0.0, AudioCandidate::Silent)); // settled
        assert_eq!(d.state, BabyState::Cooldown);
        engine.step(&evidence(5.0, 0.0, AudioCandidate::Silent));
        let d = engine.step(&evidence(6.0, 0.0, AudioCandidate::Silent));
        assert_eq!(d.state, BabyState::Idle);
    }
}
