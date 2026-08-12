//! FFI bindings for the Rockchip LLM runtime (`librkllmrt.so`).
//!
//! The library is loaded dynamically at runtime (`libloading`), so the Rust
//! binary never links against it and still builds/runs on platforms without
//! RKLLM. Model loading is heavyweight (a 1 GB+ model takes seconds and most
//! of the board's RAM), so callers load once at startup and share the handle.

use std::ffi::{c_char, c_int, c_void, CStr, CString};
use std::sync::Mutex;

use anyhow::{anyhow, Context, Result};

pub type LLMHandle = *mut c_void;

// ---- C struct layouts (mirror rkllm.h) ----

#[repr(C)]
#[derive(Clone, Copy)]
pub struct ExtendParam {
    pub base_domain_id: i32,
    pub embed_flash: i8,
    pub enabled_cpus_num: i8,
    pub enabled_cpus_mask: u32,
    pub n_batch: u8,
    pub use_cross_attn: i8,
    pub reserved: [u8; 104],
}

impl Default for ExtendParam {
    fn default() -> Self {
        Self {
            base_domain_id: 0,
            embed_flash: 0,
            enabled_cpus_num: 0,
            enabled_cpus_mask: 0,
            n_batch: 1,
            use_cross_attn: 0,
            reserved: [0; 104],
        }
    }
}

#[repr(C)]
pub struct Param {
    pub model_path: *const c_char,
    pub max_context_len: i32,
    pub max_new_tokens: i32,
    pub top_k: i32,
    pub n_keep: i32,
    pub top_p: f32,
    pub temperature: f32,
    pub repeat_penalty: f32,
    pub frequency_penalty: f32,
    pub presence_penalty: f32,
    pub mirostat: i32,
    pub mirostat_tau: f32,
    pub mirostat_eta: f32,
    pub skip_special_token: bool,
    pub ignore_eos_token: bool,
    pub is_async: bool,
    pub extend_param: ExtendParam,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct EmbedInput {
    pub embed: *mut f32,
    pub n_tokens: usize,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct TokenInput {
    pub input_ids: *mut i32,
    pub n_tokens: usize,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct MultiModalImage {
    pub image_embed: *mut f32,
    pub n_image_tokens: usize,
    pub n_image: usize,
    pub image_start: *const c_char,
    pub image_end: *const c_char,
    pub image_content: *const c_char,
    pub image_width: usize,
    pub image_height: usize,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct MultiModalVideo {
    pub video_embed: *mut f32,
    pub n_frame_tokens: usize,
    pub n_frame_per_video: usize,
    pub n_video: usize,
    pub video_start: *const c_char,
    pub video_end: *const c_char,
    pub video_content: *const c_char,
    pub frame_width: usize,
    pub frame_height: usize,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct MultiModalInput {
    pub prompt: *mut c_char,
    pub image: MultiModalImage,
    pub video: MultiModalVideo,
}

#[repr(C)]
pub union InputUnion {
    pub prompt_input: *const c_char,
    pub embed_input: EmbedInput,
    pub token_input: TokenInput,
    pub multimodal_input: MultiModalInput,
}

#[repr(C)]
pub struct Input {
    pub role: *const c_char,
    pub enable_thinking: bool,
    pub input_type: u32,
    pub value: InputUnion,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct SamplingParam {
    pub top_k: i32,
    pub top_p: f32,
    pub temperature: f32,
    pub repeat_penalty: f32,
    pub frequency_penalty: f32,
    pub presence_penalty: f32,
    pub mirostat: i32,
    pub mirostat_tau: f32,
    pub mirostat_eta: f32,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct LoraParam {
    pub lora_adapter_name: *const c_char,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct PromptCacheParam {
    pub save_prompt_cache: c_int,
    pub prompt_cache_path: *const c_char,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct InferParam {
    pub mode: u32,
    pub lora_params: *mut LoraParam,
    pub prompt_cache_params: *mut PromptCacheParam,
    pub sampling_params: *mut SamplingParam,
    pub keep_history: c_int,
    pub max_new_tokens: i32,
}

impl Default for InferParam {
    fn default() -> Self {
        Self {
            mode: 0,
            lora_params: std::ptr::null_mut(),
            prompt_cache_params: std::ptr::null_mut(),
            sampling_params: std::ptr::null_mut(),
            keep_history: 0,
            max_new_tokens: -1,
        }
    }
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct LastHiddenLayer {
    pub hidden_states: *const f32,
    pub embd_size: c_int,
    pub num_tokens: c_int,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct Logits {
    pub logits: *const f32,
    pub vocab_size: c_int,
    pub num_tokens: c_int,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct PerfStat {
    pub prefill_time_ms: f32,
    pub prefill_tokens: c_int,
    pub generate_time_ms: f32,
    pub generate_tokens: c_int,
    pub memory_usage_mb: f32,
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct RkllmResult {
    pub text: *const c_char,
    pub token_id: i32,
    pub last_hidden_layer: LastHiddenLayer,
    pub logits: Logits,
    pub perf: PerfStat,
}

pub const RUN_NORMAL: u32 = 0;
pub const RUN_WAITING: u32 = 1;
pub const RUN_FINISH: u32 = 2;
pub const RUN_ERROR: u32 = 3;

#[repr(C)]
pub struct Callback {
    pub result_callback: Option<unsafe extern "C" fn(*mut RkllmResult, *mut c_void, u32) -> c_int>,
    pub result_userdata: *mut c_void,
    pub tokenizer_callback: *const c_void,
    pub tokenizer_userdata: *mut c_void,
    pub embed_callback: *const c_void,
    pub embed_userdata: *mut c_void,
}

type InitFn = unsafe extern "C" fn(*mut LLMHandle, *mut Param, *mut Callback) -> c_int;
type RunFn = unsafe extern "C" fn(LLMHandle, *mut Input, *mut InferParam, *mut c_void) -> c_int;
type DestroyFn = unsafe extern "C" fn(LLMHandle) -> c_int;
type AbortFn = unsafe extern "C" fn(LLMHandle) -> c_int;
type IsRunningFn = unsafe extern "C" fn(LLMHandle) -> c_int;
type ClearKvCacheFn = unsafe extern "C" fn(LLMHandle, c_int, *mut c_int, *mut c_int) -> c_int;

/// A loaded RKLLM model. Only one inference runs at a time (NPU is
/// single-task); the inner mutex serializes calls.
pub struct Rkllm {
    _lib: libloading::Library,
    handle: LLMHandle,
    run: RunFn,
    destroy: DestroyFn,
    abort: AbortFn,
    is_running: IsRunningFn,
    clear_kv_cache: ClearKvCacheFn,
    lock: Mutex<()>,
    max_new_tokens: i32,
}

// The handle and function pointers are raw FFI state; the runtime is
// thread-safe as long as calls are serialized, which `lock` guarantees.
unsafe impl Send for Rkllm {}
unsafe impl Sync for Rkllm {}

/// A chunk of generated text with the callback state that produced it.
#[derive(Debug, Clone)]
pub enum Chunk {
    Text(String),
    Finished { perf: Option<(f32, i32)> },
    Error(String),
}

impl Rkllm {
    /// Loads the runtime library and initializes the model. `lib_path` may be
    /// empty to let the loader search default paths.
    pub fn load(model_path: &str, lib_path: Option<&str>) -> Result<Self> {
        let model_c =
            CString::new(model_path).map_err(|_| anyhow!("model path contains NUL byte"))?;
        unsafe {
            let lib = match lib_path {
                Some(path) if !path.is_empty() => libloading::Library::new(path),
                _ => libloading::Library::new("librkllmrt.so"),
            }
            .with_context(|| "failed to load librkllmrt.so")?;
            let init: libloading::Symbol<InitFn> = lib
                .get(b"rkllm_init")
                .context("rkllm_init symbol missing")?;
            let run: libloading::Symbol<RunFn> =
                lib.get(b"rkllm_run").context("rkllm_run symbol missing")?;
            let destroy: libloading::Symbol<DestroyFn> = lib
                .get(b"rkllm_destroy")
                .context("rkllm_destroy symbol missing")?;
            let abort: libloading::Symbol<AbortFn> = lib
                .get(b"rkllm_abort")
                .context("rkllm_abort symbol missing")?;
            let init_fn = *init;
            let run_fn = *run;
            let destroy_fn = *destroy;
            let abort_fn = *abort;
            let is_running: libloading::Symbol<IsRunningFn> = lib
                .get(b"rkllm_is_running")
                .context("rkllm_is_running missing")?;
            let clear_kv: libloading::Symbol<ClearKvCacheFn> = lib
                .get(b"rkllm_clear_kv_cache")
                .context("rkllm_clear_kv_cache missing")?;
            let is_running_fn = *is_running;
            let clear_kv_fn = *clear_kv;

            // Mirror the proven flask integration field-for-field; sampling
            // values here (temperature/top_p/repeat_penalty/n_keep) must
            // match exactly or the model degenerates into repeated tokens.
            let mut param = Param {
                model_path: model_c.as_ptr(),
                max_context_len: 4096,
                max_new_tokens: 4096,
                top_k: 1,
                n_keep: -1,
                top_p: 0.9,
                temperature: 0.8,
                repeat_penalty: 1.1,
                frequency_penalty: 0.0,
                presence_penalty: 0.0,
                mirostat: 0,
                mirostat_tau: 5.0,
                mirostat_eta: 0.1,
                skip_special_token: true,
                ignore_eos_token: false,
                is_async: false,
                extend_param: ExtendParam {
                    // RK3576/RK3588: run on the four big cores (CPU4-7) and keep
                    // the embedding table in flash to save RAM, matching the
                    // official rkllm_server demo.
                    embed_flash: 1,
                    enabled_cpus_num: 4,
                    enabled_cpus_mask: 0xF0,
                    n_batch: 1,
                    ..Default::default()
                },
            };
            let mut callback = Callback {
                result_callback: Some(result_callback),
                result_userdata: std::ptr::null_mut(),
                tokenizer_callback: std::ptr::null(),
                tokenizer_userdata: std::ptr::null_mut(),
                embed_callback: std::ptr::null(),
                embed_userdata: std::ptr::null_mut(),
            };
            let mut handle: LLMHandle = std::ptr::null_mut();
            let ret = init_fn(&mut handle, &mut param, &mut callback);
            if ret != 0 {
                return Err(anyhow!("rkllm_init failed with code {ret}"));
            }
            Ok(Self {
                _lib: lib,
                handle,
                run: run_fn,
                destroy: destroy_fn,
                abort: abort_fn,
                is_running: is_running_fn,
                clear_kv_cache: clear_kv_fn,
                lock: Mutex::new(()),
                max_new_tokens: param.max_new_tokens,
            })
        }
    }

    /// Runs one synchronous inference. Generated text (and the terminal
    /// state) are delivered through `on_chunk`; the call blocks until the
    /// model finishes or errors.
    pub fn run<'a>(
        &self,
        prompt: &str,
        role: &str,
        enable_thinking: bool,
        max_new_tokens: Option<i32>,
        on_chunk: impl FnMut(Chunk) + Send + 'a,
    ) -> Result<()> {
        // Fail fast when the previous inference is still running (e.g. the
        // runtime hung on a tricky prompt): the caller gets a clear error
        // instead of queueing forever behind a stuck model.
        let _guard = self.lock.try_lock().map_err(|_| {
            anyhow!(
                "RKLLM is busy (a previous inference may be stuck); restart the service to recover"
            )
        })?;
        let prompt_c = CString::new(prompt).map_err(|_| anyhow!("prompt contains NUL"))?;
        let role_c = CString::new(role).unwrap_or_else(|_| CString::new("user").unwrap());
        let mut on_chunk = on_chunk;
        let f_ptr: *mut (dyn FnMut(Chunk) + Send + 'a) = &mut on_chunk;
        let mut cb_box = CbBox::<'_> {
            f: f_ptr,
            error: None,
        };
        unsafe {
            let mut input = Input {
                role: role_c.as_ptr(),
                enable_thinking,
                input_type: 0, // RKLLM_INPUT_PROMPT
                value: InputUnion {
                    prompt_input: prompt_c.as_ptr(),
                },
            };
            let mut params = InferParam {
                keep_history: 0,
                max_new_tokens: max_new_tokens.unwrap_or(self.max_new_tokens),
                ..InferParam::default()
            };
            let ret = (self.run)(
                self.handle,
                &mut input,
                &mut params,
                (&mut cb_box as *mut CbBox<'a>).cast(),
            );
            if ret != 0 {
                return Err(anyhow!(
                    "rkllm_run failed with code {ret}{}",
                    cb_box
                        .error
                        .as_ref()
                        .map(|e| format!(": {e}"))
                        .unwrap_or_default()
                ));
            }
            Ok(())
        }
    }

    pub fn abort(&self) {
        drop(self.lock.lock());
        unsafe { (self.abort)(self.handle) };
    }

    pub fn is_running(&self) -> bool {
        unsafe { (self.is_running)(self.handle) == 1 }
    }

    pub fn clear_kv_cache(&self, keep_system_prompt: bool) {
        let _guard = self.lock.lock().ok();
        unsafe {
            (self.clear_kv_cache)(
                self.handle,
                keep_system_prompt as c_int,
                std::ptr::null_mut(),
                std::ptr::null_mut(),
            );
        }
    }

    pub fn max_new_tokens(&self) -> i32 {
        self.max_new_tokens
    }
}

impl Drop for Rkllm {
    fn drop(&mut self) {
        if !self.handle.is_null() {
            unsafe { (self.destroy)(self.handle) };
            self.handle = std::ptr::null_mut();
        }
    }
}

#[repr(C)]
struct CbBox<'a> {
    f: *mut (dyn FnMut(Chunk) + Send + 'a),
    error: Option<String>,
}

unsafe extern "C" fn result_callback(
    result: *mut RkllmResult,
    userdata: *mut c_void,
    state: u32,
) -> c_int {
    if userdata.is_null() {
        return 0;
    }
    let cb = &mut *(userdata as *mut CbBox<'static>);
    match state {
        RUN_FINISH | RUN_ERROR | RUN_NORMAL => {
            if !result.is_null() {
                let text = (*result).text;
                if !text.is_null() {
                    if let Ok(text) = CStr::from_ptr(text).to_str() {
                        unsafe { (&mut *cb.f)(Chunk::Text(text.to_owned())) };
                    }
                }
                if state == RUN_ERROR {
                    cb.error = Some("runtime reported RUN_ERROR".to_owned());
                }
            }
            if state == RUN_FINISH {
                let perf = if !result.is_null() {
                    let p = (*result).perf;
                    Some((p.generate_time_ms, p.generate_tokens))
                } else {
                    None
                };
                unsafe { (&mut *cb.f)(Chunk::Finished { perf }) };
            }
        }
        _ => {}
    }
    0
}

/// Convenience wrapper: runs one prompt and collects every text chunk.
pub fn run_collect(
    model: &Rkllm,
    prompt: &str,
    role: &str,
    enable_thinking: bool,
    max_new_tokens: Option<i32>,
) -> Result<String> {
    let mut out = String::new();
    model.run(prompt, role, enable_thinking, max_new_tokens, |chunk| {
        if let Chunk::Text(text) = chunk {
            out.push_str(&text);
        }
    })?;
    Ok(out)
}
