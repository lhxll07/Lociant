//! Interactive first-run setup for headless deployments (`--init`).
//!
//! Writes a JSON config file that `lociant-server` merges over stored
//! settings on every start, so a board can be configured without a UI.

use std::io::{self, BufRead, Read, Write};
use std::path::PathBuf;

pub fn run() -> anyhow::Result<()> {
    println!();
    println!("========================================");
    println!(" Lociant 初始化向导（无头模式）");
    println!("========================================");
    println!("将生成一个 config.json，服务每次启动都会把它合并进设置。");
    println!("直接回车使用 [默认值]。");
    println!();

    let port: u16 = prompt("服务器端口", "11434")
        .trim()
        .parse()
        .unwrap_or(11434);
    let host = prompt("监听地址（0.0.0.0 允许局域网访问）", "127.0.0.1");
    let token_input = prompt("API 令牌（回车自动生成 32 位随机值）", "");
    let auth_token = if token_input.trim().is_empty() {
        random_token()
    } else {
        token_input.trim().to_string()
    };
    println!();

    let backend = prompt("推理后端（none=无 / rkllm=本地 NPU）", "none")
        .trim()
        .to_ascii_lowercase();
    let mut local_model = false;
    let mut rkllm_model_path = String::new();
    let mut rkllm_lib_path = String::new();
    let mut rkllm_model_name = String::new();
    let mut peer_name = String::new();
    match backend.as_str() {
        "rkllm" | "r" => {
            local_model = true;
            rkllm_model_path = prompt(
                "RKLLM 模型文件路径（.rkllm）",
                "/home/lhx/qwen3.5-0.8b-w4a16-g128-opt0.rkllm",
            );
            rkllm_lib_path = prompt("librkllmrt.so 路径（回车用系统默认搜索）", "");
            rkllm_model_name = prompt("模型名称（API 中显示）", "qwen3.5-0.8b");
        }
        _ => {}
    }
    println!();

    let peer_token = prompt(
        "节点互联令牌（局域网内其他 Lociant 节点共享，回车不启用）",
        "",
    )
    .trim()
    .to_owned();
    let peer_discovery = prompt("启用 UDP 自动发现？（true/false）", "true")
        .trim()
        .parse::<bool>()
        .unwrap_or(true);
    if !peer_token.is_empty() {
        let default_name = std::env::var("HOSTNAME")
            .or_else(|_| std::env::var("HOST"))
            .unwrap_or_else(|_| "lociant-node".to_owned());
        peer_name = prompt("节点名称（其他设备上显示的名字）", &default_name);
    }
    println!();

    let config_path = PathBuf::from(prompt("配置文件路径", "/etc/lociant/config.json"));
    let config = serde_json::json!({
        "port": port,
        "host": host,
        "authToken": auth_token,
        "localModel": local_model,
        "rkllmModelPath": rkllm_model_path,
        "rkllmLibPath": rkllm_lib_path,
        "rkllmModelName": rkllm_model_name,
        "peerToken": peer_token,
        "peerDiscovery": peer_discovery,
        "peerName": peer_name,
    });
    let pretty = format!("{}\n", serde_json::to_string_pretty(&config)?);

    if let Some(parent) = config_path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    std::fs::write(&config_path, pretty)?;
    println!();
    println!("已写入：{}", config_path.display());
    println!("API 令牌：{}", auth_token);
    if local_model {
        println!(
            "本地 NPU（RKLLM）：{} ({})",
            rkllm_model_name, rkllm_model_path
        );
    }
    if !peer_token.is_empty() {
        println!("节点互联：已启用（令牌 ***）");
    }
    println!();
    println!("下一步（按你的部署方式选择）：");
    println!("  systemd：sudo systemctl restart lociant");
    println!(
        "  前台调试：LOCIANT_CONFIG={} lociant-server",
        config_path.display()
    );
    Ok(())
}

fn prompt(label: &str, default: &str) -> String {
    print!("{label} [{default}]: ");
    let _ = io::stdout().flush();
    let mut line = String::new();
    let _ = io::stdin().lock().read_line(&mut line);
    let value = line.trim().to_string();
    if value.is_empty() {
        default.to_string()
    } else {
        value
    }
}

pub fn random_token() -> String {
    let mut buf = [0u8; 16];
    if let Ok(mut file) = std::fs::File::open("/dev/urandom") {
        if file.read_exact(&mut buf).is_ok() {
            return buf.iter().map(|b| format!("{b:02x}")).collect();
        }
    }
    // Fallback (shouldn't happen on Linux): timestamp-based value.
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_nanos();
    format!("{nanos:032x}")
}
