//! 证据源抽象：视觉候选识别、音频候选识别、关键帧语义判断。
//!
//! 对应大创申请书三大感知模块；各平台提供实现：
//! - Linux 开发板：ffmpeg 帧差（视觉）、ALSA 能量（音频）
//! - Android：经 Lociant 设备层取摄像头/麦克风流

use slumberguard_core::{AudioCandidate, SemanticLabel, VisualCandidate};

/// 视觉候选识别：帧间运动变化 -> 运动强度与持续时间。
pub trait VisualSource: Send {
    fn read_visual(&mut self) -> anyhow::Result<VisualCandidate>;
}

/// 音频候选识别：短时能量/频段能量 -> 轻哼/哭声样/环境噪声。
pub trait AudioSource: Send {
    fn read_audio(&mut self) -> anyhow::Result<AudioCandidate>;

    /// 最近一次采集失败的原因（可选实现）。
    fn last_error(&self) -> Option<String> {
        None
    }
}

/// 关键帧语义判断（边缘侧量化视觉语言模型，MVP 占位）。
pub trait SemanticJudge: Send {
    fn judge(&mut self, timestamp: f64) -> Option<SemanticLabel>;
}

pub mod linux {
    //! Linux 平台证据源。MVP 以 ffmpeg 命令采集，后续可替换为 V4L2 直读。

    use super::{AudioSource, SemanticJudge, VisualSource};
    use slumberguard_core::{AudioCandidate, SemanticLabel, VisualCandidate};
    use std::io::Read;
    use std::process::{Command, Stdio};

    /// ffmpeg 单帧采集 + 帧差运动检测。
    ///
    /// 摄像头流式采集在该型号上不稳定（间歇卡死），改为每次采样抓取
    /// 一帧灰度图，与上一帧做像素差，计算运动强度（0-1）。
    pub struct FfmpegMotionSource {
        pub device: String,
        pub video_size: String,
        /// 判定“动作中”的运动强度阈值（用于累积持续时间）。
        pub motion_threshold: f64,
        duration_ticks: u32,
        previous: Option<Vec<u8>>,
    }

    impl FfmpegMotionSource {
        pub fn new(device: impl Into<String>) -> Self {
            Self {
                device: device.into(),
                video_size: "320x240".to_owned(),
                motion_threshold: 0.03,
                duration_ticks: 0,
                previous: None,
            }
        }
    }

    impl VisualSource for FfmpegMotionSource {
        fn read_visual(&mut self) -> anyhow::Result<VisualCandidate> {
            let frame = capture_gray_frame(&self.device, &self.video_size)?;
            let motion = match &self.previous {
                Some(previous) => frame_diff(previous, &frame),
                None => 0.0,
            };
            self.previous = Some(frame);
            if motion >= self.motion_threshold {
                self.duration_ticks += 1;
            } else {
                self.duration_ticks = 0;
            }
            Ok(VisualCandidate {
                motion_strength: motion,
                duration_ticks: self.duration_ticks,
                has_adult: false,
            })
        }
    }

    /// 麦克风能量检测：短时能量（RMS）映射为轻哼/哭声样/噪声/静音。
    ///
    /// MVP 以整体音量粗分：静音 -> 中能量 -> 高能量；哭声样与噪声的
    /// 频域区分（MFCC 等）留作后续增强。
    pub struct MicEnergySource {
        pub device: String,
        /// 采样时长（秒）。
        pub sample_seconds: f64,
        /// RMS 阈值：低于视为静音。
        pub silent_threshold: f64,
        /// RMS 阈值：高于视为哭声样（强信号）。
        pub cry_threshold: f64,
        last_error: Option<String>,
        pub last_rms: f64,
        failed_since: Option<std::time::Instant>,
    }

    impl MicEnergySource {
        pub fn new(device: impl Into<String>) -> Self {
            Self {
                device: device.into(),
                sample_seconds: 0.5,
                // 实测标定：安静 RMS≈0.0005，说话/轻哼≈0.01-0.02。
                silent_threshold: 0.008,
                cry_threshold: 0.03,
                last_error: None,
                last_rms: 0.0,
                failed_since: None,
            }
        }
    }

    impl AudioSource for MicEnergySource {
        fn last_error(&self) -> Option<String> {
            self.last_error.clone()
        }

        fn read_audio(&mut self) -> anyhow::Result<AudioCandidate> {
            // 无输入设备时 ffmpeg 会挂起；首次失败后节流，快速返回静音。
            if let Some(since) = self.failed_since {
                if since.elapsed() < std::time::Duration::from_secs(30) {
                    return Ok(AudioCandidate::Silent);
                }
                self.failed_since = None;
            }
            let pcm = match capture_pcm(&self.device, self.sample_seconds) {
                Ok(pcm) => pcm,
                Err(error) => {
                    self.failed_since = Some(std::time::Instant::now());
                    self.last_error = Some(error.to_string());
                    return Ok(AudioCandidate::Silent);
                }
            };
            self.last_error = None;
            let rms = compute_rms(&pcm);
            self.last_rms = rms;
            Ok(if rms < self.silent_threshold {
                AudioCandidate::Silent
            } else if rms > self.cry_threshold {
                AudioCandidate::CryLike
            } else {
                AudioCandidate::Hum
            })
        }
    }

    /// 用 ffmpeg 采集一段单声道 16k PCM（s16le）。
    /// 用 ffmpeg 采集一段单声道 16k PCM（s16le），带超时防止无设备挂起。
    fn capture_pcm(device: &str, seconds: f64) -> anyhow::Result<Vec<i16>> {
        let mut child = Command::new("ffmpeg")
            .args([
                "-f",
                "alsa",
                "-i",
                device,
                "-t",
                &seconds.to_string(),
                "-ac",
                "1",
                "-ar",
                "16000",
                "-f",
                "s16le",
                "-",
            ])
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()?;
        let mut stdout = child.stdout.take().expect("piped stdout");
        let reader = std::thread::spawn(move || {
            let mut buf = Vec::new();
            let _ = stdout.read_to_end(&mut buf);
            buf
        });
        let deadline = std::time::Instant::now() + std::time::Duration::from_secs(6);
        let status = loop {
            if let Some(status) = child.try_wait()? {
                break status;
            }
            if std::time::Instant::now() > deadline {
                let _ = child.kill();
                let _ = child.wait();
                anyhow::bail!("audio capture timed out (no input device: {device})");
            }
            std::thread::sleep(std::time::Duration::from_millis(50));
        };
        let bytes = reader.join().unwrap_or_default();
        if !status.success() || bytes.len() < 2 {
            anyhow::bail!("no audio captured from {device}");
        }
        Ok(bytes
            .chunks_exact(2)
            .map(|chunk| i16::from_le_bytes([chunk[0], chunk[1]]))
            .collect())
    }

    /// 16-bit PCM 的归一化 RMS（0-1）。
    fn compute_rms(samples: &[i16]) -> f64 {
        if samples.is_empty() {
            return 0.0;
        }
        let sum_sq: f64 = samples.iter().map(|s| f64::from(*s) * f64::from(*s)).sum();
        (sum_sq / samples.len() as f64).sqrt() / 32768.0
    }

    /// 语义判断占位：MVP 不接入 VLM，返回 None（由状态机按视觉/音频融合）。
    pub struct NoopSemanticJudge;

    impl SemanticJudge for NoopSemanticJudge {
        fn judge(&mut self, _timestamp: f64) -> Option<SemanticLabel> {
            None
        }
    }

    /// 抓取一帧灰度图（ffmpeg 输出原始 gray 帧到 stdout）。
    fn capture_gray_frame(device: &str, video_size: &str) -> anyhow::Result<Vec<u8>> {
        let output = Command::new("ffmpeg")
            .args([
                "-f",
                "v4l2",
                "-input_format",
                "mjpeg",
                "-video_size",
                video_size,
                "-i",
                device,
                "-f",
                "rawvideo",
                "-pix_fmt",
                "gray",
                "-frames:v",
                "1",
                "-",
            ])
            .output()?;
        anyhow::ensure!(
            !output.stdout.is_empty(),
            "no frame captured from {}: {}",
            device,
            String::from_utf8_lossy(&output.stderr).trim()
        );
        Ok(output.stdout)
    }

    /// 两帧灰度像素差的平均比例（0-1）。
    fn frame_diff(a: &[u8], b: &[u8]) -> f64 {
        if a.len() != b.len() || a.is_empty() {
            return 1.0;
        }
        let sum: u64 = a
            .iter()
            .zip(b)
            .map(|(x, y)| (*x as i16 - *y as i16).unsigned_abs() as u64)
            .sum();
        sum as f64 / (a.len() as f64 * 255.0)
    }

    #[cfg(test)]
    mod tests {
        use super::*;

        #[test]
        fn frame_diff_zero_for_identical() {
            let frame = vec![10u8; 100];
            assert_eq!(frame_diff(&frame, &frame), 0.0);
        }

        #[test]
        fn frame_diff_max_for_black_white() {
            let a = vec![0u8; 100];
            let b = vec![255u8; 100];
            assert!((frame_diff(&a, &b) - 1.0).abs() < 1e-9);
        }

        #[test]
        fn frame_diff_handles_size_mismatch() {
            assert_eq!(frame_diff(&[1u8], &[1u8, 2u8]), 1.0);
        }

        #[test]
        fn rms_zero_for_silence() {
            assert_eq!(compute_rms(&[0i16; 100]), 0.0);
        }

        #[test]
        fn rms_positive_for_signal() {
            let samples = vec![10000i16; 100];
            let rms = compute_rms(&samples);
            assert!(rms > 0.2 && rms < 0.4, "rms={rms}");
        }

        #[test]
        fn rms_full_scale_is_one() {
            let samples = vec![i16::MAX; 64];
            assert!((compute_rms(&samples) - 1.0).abs() < 1e-3);
        }
    }
}
