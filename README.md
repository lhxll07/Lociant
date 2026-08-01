# Lociant

让安卓手机成为一个真正能干活的本地 Agent：它可以在手机上运行模型，读取屏幕、打开 App、查看信息、操作界面，也可以通过 MCP 或 OpenAI 兼容接口被电脑上的 Agent 调用。

它适合把旧手机重新利用起来：手机负责模型和实际操作，电脑只负责对话、规划和编排。数据默认留在本地，能力由你在手机里逐项授权。

## 能做什么

- 在手机本地运行 LLM / VLM 模型，进行对话和图片理解。
- 查看屏幕、读取界面、点击、滑动、返回和打开 App。
- 读取设备状态和剪贴板，调用摄像头进行拍摄或视觉分析。
- 通过 MCP 给 Claude、Codex 或其他 Agent 使用，也支持 OpenAI 兼容 API。

例如：让 Agent 打开 QQ 看未读消息、总结 B 站动态、查一个 App 里的信息，或者帮你测试刚装好的新应用。

## 一步一步开始

### 1. 安装

当前发布包适用于 Android 8.0 及以上、`arm64-v8a` 设备：

[下载 Lociant v1.0.1 APK](https://github.com/lhxll07/Lociant/releases/download/v1.0.1/lociant-1.0.1-arm64-v8a-release.apk)

安装时如果系统提示允许安装未知来源应用，请按系统提示允许即可。

### 2. 打开权限

第一次打开 Lociant，进入“设置”，按需要开启权限：

1. 开启“无障碍”：让 Lociant 读取屏幕并执行点击、滑动等操作。
2. 允许通知：保持后台运行时显示服务状态。
3. 允许相机：需要拍照或视觉分析时再开启。
4. 允许悬浮窗：需要在其他 App 上方显示运行状态时开启。
5. 将电池策略设为“不限制”：避免手机锁屏后暂停服务。

只聊天和调用普通接口时不需要全部权限；涉及屏幕和 UI 操作时，无障碍权限是关键。

### 3. 安装模型

进入“模型”页，选择一个模型并点击“安装”，等待下载和初始化完成。安装完成后选择它作为默认模型。

模型会占用手机存储和内存。旧设备建议先选小模型，第一次运行时保持手机亮屏并接入电源。

### 4. 启动运行时

回到首页，点击启动运行时。看到状态变为“运行中”后，先在 Lociant 内发一条简单消息确认模型正常，再开始调用手机能力。

默认服务地址是：

```text
http://手机IP:11434
```

手机 IP 可以在手机的 Wi-Fi 详情里查看。电脑和手机需要连接同一个局域网。

### 5. 连接 MCP Agent

在“设置”中生成 API 令牌，然后把下面的配置加入支持 MCP 的客户端：

```json
{
  "mcpServers": {
    "lociant": {
      "type": "streamable-http",
      "url": "http://手机IP:11434/mcp",
      "headers": {
        "Authorization": "Bearer 你的API令牌"
      }
    }
  }
}
```

连接后，Agent 就可以发现 Lociant 暴露的手机工具。想让它执行打开 App、点击和滑动等操作，请在“设置 → 远程工具”中选择“操作”；只查看状态时选择“读取”即可。

## 安全提醒

Lociant 面向局域网使用，当前接口使用 HTTP。请设置 API 令牌，不要把 `11434` 端口直接暴露到公网，也不要把令牌发给不信任的客户端。

## 开发者

Lociant 使用 Kotlin、MNN、NCNN 和 Ktor 构建。开发、构建和完整接口说明见：

- [Android 开发说明](apps/android/README.md)
- [Agent 与 MCP 接入](docs/agent-integration.md)
- [OpenAI 兼容 API](docs/openai-compatible.md)
- [控制 API](docs/control-api.md)

## 许可证

[MIT License](LICENSE)
