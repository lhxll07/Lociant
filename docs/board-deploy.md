# 部署到 RK 开发板（无头模式）

把 Lociant 跑在 RK 开发板上：Rust 后端作为 systemd 服务常驻，RKLLM 推理
已内置于后端（通过 librkllmrt.so 动态加载），用 NPU 跑本地模型。本教程以
RK3576（Armbian）为例，适用于同类 RK 板子。全程不需要显示器，SSH 即可
完成，不需要 Python 环境或额外的推理进程。

## 你需要的

- 一台装好 Armbian 的 RK 开发板（SSH 可达，2GB 内存以上）
- 一台 PC（Linux），装有 Rust 工具链和 `aarch64-linux-gnu-gcc`
- 本地模型：`.rkllm` 文件（如用 rknn-llm-gui 转换的 W4A16 模型）

PC 上先装交叉编译工具链：

```bash
rustup target add aarch64-unknown-linux-gnu
# Arch: sudo pacman -S aarch64-linux-gnu-gcc
```

## 第 1 步：交叉编译 Rust 后端

在仓库根目录执行：

```bash
cd apps/rust-backend
CC_aarch64_unknown_linux_gnu=aarch64-linux-gnu-gcc \
CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc \
cargo build --release --target aarch64-unknown-linux-gnu
```

产物：`apps/rust-backend/target/aarch64-unknown-linux-gnu/release/lociant-server`

## 第 2 步：上传并安装服务

把二进制和安装脚本传到板子：

```bash
scp apps/rust-backend/target/aarch64-unknown-linux-gnu/release/lociant-server \
    lhx@192.168.10.103:/tmp/
scp deploy/install.sh lhx@192.168.10.103:/tmp/
```

SSH 到板子上安装（会自动注册 systemd 服务并启动）：

```bash
ssh lhx@192.168.10.103
sudo bash /tmp/install.sh /tmp/lociant-server
```

## 第 3 步：初始化配置（新手引导）

在板子上运行交互式初始化向导，按提示填写端口、监听地址、API 令牌和推理
后端（`none` / `cloud` / `rkllm`）。选 `rkllm` 会继续询问模型文件路径、
`librkllmrt.so` 路径（回车用系统默认搜索）和模型名称，自动生成
`rkllmModelPath` / `rkllmModelName` / `localModel: true` 配置：

```bash
sudo /usr/local/bin/lociant-server --init
```

它会生成 `/etc/lociant/config.json`。向导结束后重启服务生效：

```bash
sudo systemctl restart lociant
curl http://127.0.0.1:11434/health
```

返回 `ok` 说明服务已就绪。局域网访问需要监听 `0.0.0.0`（向导第二步）。

## 第 4 步：接入 RKLLM 本地推理

模型文件（`.rkllm`）上传到板子后（如 `~/qwen3.5-0.8b-w4a16-g128-opt0.rkllm`），
在 `--init` 向导里选 `rkllm` 后端并填路径，或直接编辑
`/etc/lociant/config.json` 添加关键字段：

```json
{
  "rkllmModelPath": "/home/lhx/qwen3.5-0.8b-w4a16-g128-opt0.rkllm",
  "rkllmLibPath": "/home/lhx/rknn-llm/rknn-llm/rkllm-runtime/Linux/librkllm_api/aarch64/librkllmrt.so",
  "rkllmModelName": "qwen3.5-0.8b-w4a16-g128-opt0",
  "localModel": true
}
```

`rkllmLibPath` 可留空让系统搜索 `librkllmrt.so`；`localModel: true` 让本地
推理默认不带工具定义，省 token。重启服务并确认加载：

```bash
sudo systemctl restart lociant
sudo journalctl -u lociant -n 30 --no-pager | grep -i "rkllm"
```

日志出现 `RKLLM loaded: ...` 即为成功。加载行里的 `model_dtype` 会显示实际
量化类型（W4A16 需在转换时用 `optimization_level=0`，否则会静默回退 W8A8）。

## 第 5 步：验证

在板子上测一条聊天：

```bash
curl http://127.0.0.1:11434/v1/chat/completions \
  -H "Authorization: Bearer 你的令牌" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen3.5-0.8b-w4a16-g128-opt0","messages":[{"role":"user","content":"你好"}],"stream":false}'
```

电脑或手机连接板子：Flutter UI 启动时加
`--dart-define=LOCIANT_BASE_URL=http://板子IP:11434`，或在设置页把服务器
地址改为板子 IP。外部 Agent 走 `http://板子IP:11434/mcp`。

## 多节点互联

所有设备配置**相同的节点令牌**后会自动互相发现（mDNS，无需手动添加）。
在 `--init` 向导里会询问节点令牌，或手动编辑 `/etc/lociant/config.json`：

```json
{
  "peerToken": "所有设备共享的令牌",
  "peerName": "RK3576-Board"
}
```

- 节点列表：Flutter UI 的"节点"页（与模型/设置平级），或
  `curl http://板子IP:11434/api/v1/nodes`。
- 互借模型：其他节点发现的模型会以 `peer:节点id:模型id` 出现在模型页，
  直接选用即可（例如本机用板子的 RKLLM 模型）。
- 互借工具：对等节点调用走 `/api/v1/peer/*`，暴露级别由**提供方**自己
  的 `toolExposure` 决定。

## 终端客户端（TUI）

无头板子没有 UI 也能直接用终端聊天和管理。SSH 到板子后：

```bash
lociant-tui                      # 连本机 127.0.0.1:11434
lociant-tui --connect http://192.168.10.252:11434 --token 你的令牌
```

界面：顶部状态栏（在线状态/当前模型/节点数），中间对话区，底部输入框。
命令：`/help`、`/models`、`/model <id>`、`/nodes`、`/clear`、`/quit`。

## 常见问题

- **局域网连不上**：确认监听地址是 `0.0.0.0`、systemd 里
  `LOCIANT_HOST` 未被覆盖，并检查防火墙。
- **内存吃紧**：RK3576 只有 2GB，建议开 zram/swap。W4A16 量化能省约
  300MB 内存。
- **模型加载慢/失败**：确认 `.rkllm` 的 `target_platform` 与板子一致
  （如 rk3576），`rkllmLibPath` 指向板子上真实的 `librkllmrt.so`（或留空
  让系统搜索），并用 `journalctl -u lociant` 查看 `rkllm init failed` 的
  具体原因。
- **想改配置**：直接编辑 `/etc/lociant/config.json` 后
  `sudo systemctl restart lociant`，配置每次启动都会重新合并。
