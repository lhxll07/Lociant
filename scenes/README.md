# Scenes

[English](#english) · [中文](#中文)

## English

This directory contains MNNode Scene Pack source files.

A scene pack is a small installable unit:

```text
manifest.json
web/index.html
```

Current scenes:

| Scene | Purpose | Notes |
|---|---|---|
| `study-desk/` | Focus tracking | Uses YOLO person / phone detection and unified chat/image model calls. |
| `baby-guard/` | Person-presence guard | Minimal caretaker scene; uses `person` as the first stand-in target. |
| `model-server/` | Runtime panel | Built-in control panel for the core OpenAI/Ollama-style LAN model API. |

Scene packs can be copied into Android assets for built-in use, or packaged as zip files for external installation.

`model-server` is kept in this directory for UI packaging, but it is a system runtime panel rather than a typical third-party Scene Pack.

Example packaging command from the project root:

```powershell
tar -a -cf dist/scenes/baby-guard.scene.zip -C scenes/baby-guard manifest.json web/index.html
```

Keep zip entries flat under the scene root. The installer expects `manifest.json` and `web/index.html`, not an extra top-level directory.

## 中文

这个目录存放 MNNode Scene Pack 源文件。

场景包是一个小型可安装单元：

```text
manifest.json
web/index.html
```

当前场景：

| 场景 | 用途 | 说明 |
|---|---|---|
| `study-desk/` | 专注跟踪 | 使用 YOLO person / phone 检测和统一 chat/image 模型调用。 |
| `baby-guard/` | 人员存在看护 | 最小看护场景；第一版用 `person` 作为目标。 |
| `model-server/` | Runtime 面板 | 核心 OpenAI/Ollama 风格局域网模型 API 的内置控制面板。 |

场景包可以复制到 Android assets 作为内置场景，也可以打包成 zip 用于外部安装。

`model-server` 仍放在这个目录中以复用 UI 打包方式，但它是系统 runtime 面板，不是普通第三方 Scene Pack。

从项目根目录打包示例：

```powershell
tar -a -cf dist/scenes/baby-guard.scene.zip -C scenes/baby-guard manifest.json web/index.html
```

zip 内部路径要保持扁平。安装器期望看到 `manifest.json` 和 `web/index.html`，不要多包一层顶级目录。
