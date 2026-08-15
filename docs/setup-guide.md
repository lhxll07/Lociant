# Lociant 配置指南（从零开始）

Lociant 是边缘设备运行时和控制台。它把本地模型、设备工具、传感器和多
节点连接集中到一个受策略保护的运行时中，不提供主页聊天或通用 Agent loop。

## 设备形态

| 设备 | 运行方式 | 适合场景 |
|---|---|---|
| 安卓手机 | Flutter 控制台 + Rust 服务 + Kotlin 设备层 | GGUF/llama.cpp、无障碍、传感器、相机 |
| Linux 桌面 | Flutter 控制台 + Rust sidecar | 本地文件/进程工具和节点控制 |
| RK 开发板 | systemd + Rust 服务 + RKLLM + TUI | 7x24 常驻、低功耗边缘节点 |

所有形态都提供相同的控制 API 和 MCP 入口。模型和工具的实际执行留在
拥有硬件的节点上。

## 一、安卓手机

### 1.1 安装

下载并安装 arm64-v8a APK（Android 8.0+）：

[下载 Lociant v2.0.1 APK](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-arm64-v8a-release.apk)

源码构建：

```bash
bash scripts/dev-install.sh
```

### 1.2 首次配置

第一次打开会进入引导，也可以在设置中再次打开。按需授予以下权限：

1. 无障碍：读取屏幕并执行点击、滑动和输入。
2. 通知：保持后台运行并显示服务状态。
3. 相机：启用视觉和相机工具。
4. 悬浮窗：显示设备运行状态窗口。
5. 电池策略：将后台运行设为不限制，避免服务被系统暂停。

在设置中配置 API 令牌和远程工具暴露级别。令牌为空时只适合可信局域网。
暴露级别从 `read`、`sensor` 到 `action` 逐级增加能力。

### 1.3 本地模型

打开“模型”，导入 `.gguf` 文件或包含单个 `.gguf` 的压缩包。安装完成后
在运行时视图中选择默认模型；llama.cpp 会在本地运行该 GGUF。模型文件位于
应用的共享模型目录，Rust 后端
通过 Android IPC 读取同一份模型清单。

### 1.4 连接设备

启动运行时后，控制台首页会显示运行状态、模型、工具和节点。设备的局域网
地址类似：

```text
http://手机IP:11434
```

MCP 地址为 `http://手机IP:11434/mcp`，控制 API 为
`http://手机IP:11434/api/v1`。

## 二、Linux 桌面

### 2.1 发布包

Debian / Ubuntu：

```bash
sudo apt install ./lociant_2.0.1_amd64.deb
lociant
```

其他发行版：

```bash
tar -xzf lociant-2.0.1-linux-x86_64.tar.gz
cd lociant-2.0.1-linux-x86_64
./lociant_flutter
```

源码运行：

```bash
cd apps/rust-backend && cargo run
cd apps/flutter && flutter run -d linux
```

Flutter 会把 Rust 后端作为 sidecar 启动。Linux 默认工具包括文件系统和
进程工具，工具暴露和执行都经过 `ToolRegistry`。

## 三、RK 开发板

### 3.1 获取二进制

下载 aarch64 发布包：

[下载 Lociant v2.0.1 Linux aarch64](https://github.com/lhxll07/Lociant/releases/download/v2.0.1/lociant-2.0.1-linux-aarch64.tar.gz)

包内包含 `lociant-server`、`lociant-tui` 和部署文件。源码交叉编译：

```bash
rustup target add aarch64-unknown-linux-gnu
cd apps/rust-backend
CC_aarch64_unknown_linux_gnu=aarch64-linux-gnu-gcc \
CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc \
cargo build --release --target aarch64-unknown-linux-gnu
```

### 3.2 配置

编辑 `/etc/lociant/config.json`：

```json
{
  "authToken": "replace-with-a-local-token",
  "peerToken": "shared-lan-token",
  "peerName": "RK3576-Board",
  "host": "0.0.0.0",
  "port": 11434,
  "rkllmModelPath": "/opt/models/qwen.rkllm",
  "rkllmModelName": "qwen-local"
}
```

关键字段：

- `authToken`：控制 API 和 MCP 令牌。
- `peerToken`：节点互联共享令牌，留空则关闭 mesh。
- `host`：局域网访问使用 `0.0.0.0`，默认只监听本机。
- `rkllmModelPath`：RKLLM 模型文件路径。
- `rkllmLibPath`：`librkllmrt.so` 路径，可留空使用系统搜索。
- `peerDiscovery`：设为 `false` 可关闭 UDP 自动发现。

也可以运行初始化向导：

```bash
sudo lociant-server --init
sudo systemctl restart lociant
```

### 3.3 运行和检查

```bash
sudo systemctl restart lociant
curl http://127.0.0.1:11434/health
lociant-tui --connect http://127.0.0.1:11434 --token YOUR_TOKEN
```

TUI 是边缘节点控制台，不是聊天客户端。它展示运行时、模型、节点和工具，
支持 `/refresh`、`/models`、`/nodes`、`/tools`、`/help` 和 `/quit`。

RKLLM 加载日志：

```bash
sudo journalctl -u lociant -n 50 --no-pager | grep -i rkllm
```

W4A16 模型导出应使用 `optimization_level=0`，并确认日志显示目标平台和
实际量化类型。

## 四、多节点互联

同一局域网的设备设置相同 `peerToken` 后会通过 UDP 自动发现。节点页和
`GET /api/v1/nodes` 可以查看节点；其他节点的工具和模型会以 peer 名称出现。

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://NODE_IP:11434/api/v1/nodes
```

节点工具调用走 `/api/v1/peer/*`，提供方的暴露级别和 `remoteAllowed` 仍然
会在执行前校验。令牌不同、网络隔离或 `peerDiscovery=false` 时可以手动添加
节点。

## 五、MCP 接入

使用以下地址接入 Claude、Codex、OpenCode、RikkaHub 或自建客户端：

```text
http://设备IP:11434/mcp
```

设置 `Authorization: Bearer YOUR_TOKEN`。连接后执行 `tools/list`，再通过
`tools/call` 调用设备工具。建议先使用 `read` 暴露级别验证连接，再按需启用
传感器和操作能力。

完整协议和控制 API 见 [边缘运行时接入文档](agent-integration.md)。

## 六、验证脚本

脚本验证健康检查、认证、模型清单、工具调用和 MCP：

```bash
python scripts/lociant_test.py quick \
  --base-url http://设备IP:11434 \
  --api-key YOUR_TOKEN \
  --expect-auth
```

## 安全

Lociant 面向局域网使用，接口使用明文 HTTP。不要把 `11434` 直接暴露到公网。
绑定 `0.0.0.0` 前设置 API 令牌，并为 mesh 使用单独的 `peerToken`。

## 参考

- [架构](architecture.md)
- [边缘运行时接入](agent-integration.md)
- [Android 开发说明](../apps/android/README.md)
