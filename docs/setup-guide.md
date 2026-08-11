# Lociant 配置指南（从零开始）

这份指南面向第一次使用 Lociant 的人，覆盖全部三种玩法：安卓手机、Linux
桌面和 RK 开发板（无头模式）。按你的设备挑对应的章节，照着做即可。

## 三种玩法，选一个

| 设备 | 玩法 | 适合场景 |
|---|---|---|
| 安卓手机 | 完整 Agent：本地/云端模型 + 手机工具（看屏、点击、传感器、相机） | 把旧手机变成能干活的 Agent |
| Linux 桌面（x86_64） | Flutter UI + 内置 Rust 后端（sidecar） | 日常聊天、编排、连接板子/手机 |
| RK 开发板（Armbian） | 无头服务 + NPU 本地推理（RKLLM）+ 终端 TUI | 7×24 常驻、低功耗、婴儿监控等 |

设备之间可以互联：手机、电脑、板子配同一个“节点令牌”后会自动发现彼此，
互相借用模型和工具。详见[多节点互联](#多节点互联)。

---

## 一、安卓手机

### 1.1 安装

下载最新 APK 并安装（Android 8.0+，`arm64-v8a`）：

[下载 Lociant v2.0.0 APK](https://github.com/lhxll07/Lociant/releases/download/v2.0.0/lociant-2.0.0-arm64-v8a-release.apk)

安装时系统提示“允许安装未知来源应用”，按提示允许即可。

### 1.2 新手引导与权限

第一次打开会自动进入新手引导，按引导配置即可。之后可在“设置 → 新手引导”
重新进入。需要手动检查的权限（在“设置”里逐项开启）：

1. **无障碍**：让 Lociant 读取屏幕并执行点击、滑动等操作（这是“动手”能力的关键）。
2. **通知**：保持后台运行时显示服务状态。
3. **相机**：需要拍照或视觉分析时再开。
4. **悬浮窗**：在其他 App 上方显示运行状态时再开。
5. **电池策略 → 不限制**：避免手机锁屏后服务被暂停。

只聊天和调用普通接口时不需要全部权限；涉及屏幕和 UI 操作时，无障碍是关键。

### 1.3 配置云端模型（可选，但推荐先用它跑通）

进入“设置 → 云端模型”，填入：

- **服务地址**：OpenAI 兼容接口，如 `https://api.deepseek.com/v1`
- **API Key**：你的密钥
- **模型名**：如 `deepseek-chat`

云端模型的好处是不占手机存储，先用它把 Agent 跑通，再考虑装本地模型。

### 1.4 安装本地模型（可选）

进入“模型”页，选一个模型点“安装”，等下载和初始化完成，再把它设为默认。

> 模型会占存储和内存。旧设备建议先选小模型；第一次运行保持亮屏并接电源。

### 1.5 启动运行时

回到首页，点“启动运行时”。状态变为“运行中”后，先发一条简单消息确认模型
正常，再让它干活。默认服务地址：

```text
http://手机IP:11434
```

手机 IP 在 Wi-Fi 详情里查看；电脑和手机需在同一局域网。

### 1.6 把手机能力接给外部 Agent（MCP）

手机、电脑、板子共用同一个 MCP 入口（`http://设备IP:11434/mcp`），具体
配置方法见[第五章：MCP](#五mcp把设备能力接给外部-agent)。

---

## 二、Linux 桌面（x86_64）

### 2.1 安装

下载并解压（包内已内置 Rust 后端，无需额外安装服务）：

```bash
tar -xzf lociant-2.0.0-linux-x86_64.tar.gz
cd lociant-2.0.0-linux-x86_64
./lociant_flutter
```

首次运行可能需要 GTK3 依赖：

```bash
# Arch / Manjaro
sudo pacman -S gtk3
# Debian / Ubuntu
sudo apt install libgtk3-0
```

> ARM64 设备（如 RK 开发板）没有桌面 UI，请改用 **aarch64 发布包**——
> 它包含 `lociant-server`、`lociant-tui` 和部署脚本，见
> [第三章](#三rk-开发板无头模式--rkllm)。

### 2.2 启动后

桌面 App 打开时会自动拉起内置的 Rust 后端（sidecar，端口 `11434`，监听
局域网以便手机/板子连接）。界面和安卓一样：首页聊天、“模型”页、“节点”页、
“设置”页。

- 云端模型：设置 → 云端模型（同安卓 1.3）。
- 本地模型：桌面上暂时没有本地推理后端，先使用云端模型。
- 作为节点：桌面端会广播自己，手机/板子配相同节点令牌后即可互连。

### 2.3 从源码运行（开发者）

```bash
cd apps/rust-backend && cargo run          # 后端 http://127.0.0.1:11434
cd apps/flutter && flutter run -d linux    # UI
```

需要 Rust 工具链和 Flutter SDK。

---

## 三、RK 开发板（无头模式 + RKLLM）

全程不需要显示器，SSH 即可。以 RK3576 / Armbian 为例，适用于同类 RK 板子。

### 3.1 你需要的

- 一台装好 Armbian 的开发板（SSH 可达，2GB 内存以上）
- 一台 PC（Linux）用于上传文件（自己改代码编译时才需要 Rust 工具链和
  `aarch64-linux-gnu-gcc`，见 3.2）
- 一个 `.rkllm` 模型文件（转换方法见 3.4）

### 3.2 获取后端和 TUI

**推荐：直接下载 aarch64 发布包**（已按板子架构编译好，含 server、TUI
和部署脚本）：

[下载 Lociant v2.0.0 Linux aarch64](https://github.com/lhxll07/Lociant/releases/download/v2.0.0/lociant-2.0.0-linux-aarch64.tar.gz)

```bash
tar -xzf lociant-2.0.0-linux-aarch64.tar.gz
cd lociant-2.0.0-linux-aarch64
```

改了源码需要自己编译时，在 PC 上先装交叉编译工具链：

```bash
rustup target add aarch64-unknown-linux-gnu
# Arch: sudo pacman -S aarch64-linux-gnu-gcc
```

然后进入 `apps/rust-backend` 编译，产物在
`target/aarch64-unknown-linux-gnu/release/` 下（`lociant-server` 和
`lociant-tui`）：

```bash
CC_aarch64_unknown_linux_gnu=aarch64-linux-gnu-gcc \
CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc \
cargo build --release --target aarch64-unknown-linux-gnu
```

### 3.3 上传并安装服务

把二进制和安装脚本传到板子：

```bash
# 从发布包目录执行（自己编译的话，把路径换成 target/.../release 下的产物）
scp lociant-server lociant-tui deploy/install.sh 用户@板子IP:/tmp/
```

SSH 到板子上安装（自动注册 systemd 服务并启动）：

```bash
ssh 用户@板子IP
sudo bash /tmp/install.sh /tmp/lociant-server
sudo cp /tmp/lociant-tui /usr/local/bin/
```

> 注意：`deploy/lociant.service` 里 `User/Group` 默认是 `lhx`。用户名不同
> 的话，先编辑它（或运行 `sudo systemctl edit lociant` 覆盖），并确保
> `/home/你的用户名/lociant/data` 目录存在。

### 3.4 初始化配置

编辑 `/etc/lociant/config.json`（安装脚本已生成默认文件）：

```json
{
  "authToken": "改成你自己的令牌",
  "peerToken": "所有设备共享的节点令牌",
  "peerName": "RK3576-Board",
  "host": "0.0.0.0",
  "port": 11434,
  "rkllmModelPath": "/home/你的用户名/qwen3.5-0.8b.rkllm",
  "rkllmModelName": "qwen3.5-0.8b",
  "localModel": true
}
```

字段说明：

- `authToken`：访问 API 的令牌，电脑/手机/外部 Agent 都要带它。
- `peerToken`：多节点互联用的共享令牌，所有设备必须一致。
- `host: 0.0.0.0`：允许局域网访问（默认只监听本机）。
- `rkllmModelPath`：`.rkllm` 模型文件路径。
- `rkllmLibPath`：`librkllmrt.so` 路径，一般可留空让系统自动搜索。
- `localModel: true`：本地推理默认不带工具定义，省 token。

保存后重启并确认健康：

```bash
sudo systemctl restart lociant
curl http://127.0.0.1:11434/health
```

返回 `ok` 即服务就绪。也可以用交互式向导代替手写配置：

```bash
sudo /usr/local/bin/lociant-server --init
sudo systemctl restart lociant
```

### 3.5 RKLLM 本地模型（NPU 推理）

模型文件一般用 rknn-llm-gui 转换。要点：

- 板子（如 RK3576）对应的 `target_platform` 要和模型转换时一致。
- **W4A16 量化请用 `optimization_level=0` 导出**，否则会静默回退 W8A8
  （两者文件大小差不多，光看大小分辨不出来）。
- 上传模型后（如 `~/qwen3.5-0.8b.rkllm`），按 3.4 填好
  `rkllmModelPath`，重启服务。

确认加载成功：

```bash
sudo journalctl -u lociant -n 30 --no-pager | grep -i rkllm
```

出现 `RKLLM loaded: ...` 即为成功；日志里的 `model_dtype` 会显示实际量化
类型（W4A16 应显示 `W4A16_G128`）。

验证一条聊天：

```bash
curl http://127.0.0.1:11434/v1/chat/completions \
  -H "Authorization: Bearer 你的令牌" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen3.5-0.8b","messages":[{"role":"user","content":"你好"}],"stream":false}'
```

### 3.6 用 TUI 在终端聊天

无头板子没有 UI，直接用终端聊天：

```bash
lociant-tui                        # 连本机 127.0.0.1:11434
lociant-tui --connect http://板子IP:11434 --token 你的令牌   # 从任意机器连
```

界面：顶部状态栏（在线状态/当前模型/节点数），中间对话区，底部输入框。
命令：

```text
/help    /models    /model <id>    /nodes    /clear    /quit
```

### 3.7 常见问题

- **局域网连不上**：确认监听地址是 `0.0.0.0`、systemd 里没被覆盖，并检查防火墙。
- **内存吃紧**：RK3576 只有 2GB，建议开 zram/swap；W4A16 能省约 300MB。
- **模型加载失败**：确认 `.rkllm` 的 `target_platform` 与板子一致、库路径
  正确，再看 `journalctl -u lociant` 里 `rkllm init failed` 的具体原因。
- **改配置不生效**：`/etc/lociant/config.json` 每次启动都会重新合并，
  改完记得 `sudo systemctl restart lociant`。

---

## 四、多节点互联

手机、电脑、板子在同一局域网，且配置**相同的 `peerToken`**，即可自动发现
彼此（UDP 广播，无需手动配置）。节点令牌不同或网络隔离时，可以在“节点”页
手动添加（主机:端口）。

- **看节点**：UI 的“节点”页，或 `curl http://节点IP:11434/api/v1/nodes`。
- **互借模型**：其他节点的模型会以 `peer:节点id:模型id` 出现在“模型”页，
  直接选用即可（例如手机用板子的 RKLLM 模型）。
- **互借工具**：对等调用走 `/api/v1/peer/*`，能暴露什么由**提供方**自己的
  “远程工具”级别（读取/传感器/操作）决定。

---

## 五、MCP：把设备能力接给外部 Agent

MCP 是一个标准接口：Claude、OpenCode 等外部 Agent 通过它“看到”并调用
Lociant 的设备工具（看屏幕、点击、传感器、相机、模型等）。手机、板子、
电脑都能作为 MCP 服务器，入口统一是：

```text
http://设备IP:11434/mcp
```

### 5.1 准备工作

1. **设置 API 令牌**：手机/电脑在“设置”里生成或填写 API 令牌；板子改
   `/etc/lociant/config.json` 的 `authToken`（见 3.4）。
2. **选择工具暴露级别**（“设置 → 远程工具”）：
   - `读取`：只读状态和模型信息，最安全；
   - `传感器`：加上传感器和屏幕上下文；
   - `操作`：全部能力，包括点击、滑动等改动设备状态的操作。
3. **确认设备地址**：手机/板子的 IP 在“设置”或 Wi-Fi 详情里看（界面上的
   `lanUrl` 就是）。电脑用 `http://127.0.0.1:11434` 或局域网 IP。

### 5.2 通用配置（任意支持 MCP 的客户端）

以 Claude Desktop 等支持 `mcpServers` 的客户端为例，把下面这段加入客户端
配置，替换 `设备IP` 和 `你的API令牌`：

```json
{
  "mcpServers": {
    "lociant": {
      "type": "streamable-http",
      "url": "http://设备IP:11434/mcp",
      "headers": { "Authorization": "Bearer 你的API令牌" }
    }
  }
}
```

### 5.3 OpenCode（电脑）

在 `opencode.json` 中加入（`remote` 是固定类型）：

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "lociant": {
      "type": "remote",
      "url": "http://设备IP:11434/mcp",
      "enabled": true,
      "timeout": 120000,
      "headers": {
        "Authorization": "Bearer 你的API令牌",
        "Accept": "application/json, text/event-stream"
      }
    }
  }
}
```

> `timeout` 必须调大：Lociant 的本地工具（如 `llm_chat`、
> `ui_screen_state`）经常超过 OpenCode 默认的 5 秒超时。

### 5.4 RikkaHub（手机）

设置 → MCP → 导入，粘贴以下 JSON（`streamable_http` 的 `headers` 是
`[名称, 值]` 对数组）：

```json
{
  "type": "streamable_http",
  "commonOptions": {
    "name": "Lociant 设备工具",
    "enable": true,
    "headers": [
      ["Authorization", "Bearer 你的API令牌"]
    ]
  },
  "url": "http://设备IP:11434/mcp"
}
```

导入后在“助手设置 → MCP 服务器”中勾选它；对会改动设备状态的工具建议
开启“需要审批”。

### 5.5 验证是否接上

连接后先问外部 Agent 一句：“你有哪些工具？”它应该能列出 Lociant 的
`runtime_status`、`ui_*`、`sensor_*`、`camera_capture` 等工具。也可以在
电脑上跑官方探测脚本做完整验证：

```bash
python scripts/lociant_test.py full \
  --base-url http://设备IP:11434 \
  --api-key 你的API令牌 \
  --expect-auth
```

工具清单、调用参数等接口细节见 [Agent 接入文档](agent-integration.md)。

---

## 安全提醒

Lociant 面向局域网使用，接口为 HTTP。请务必设置 API 令牌，不要把 `11434`
端口直接暴露到公网，也不要向不信任的客户端提供令牌。

---

## 开发者与接口参考

- [架构](architecture.md)
- [Agent 与 MCP 接入](agent-integration.md)
- [控制 API](control-api.md)
- [Android 开发说明](../apps/android/README.md)
