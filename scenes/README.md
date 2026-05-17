# Scenes

[English](#english) | [中文](#中文)

## English

This directory contains Lociant Scene Pack source files.

A Scene Pack is a small installable phone-local experience:

```text
manifest.json
web/index.html
```

Current built-in scene:

| Scene | Purpose | Notes |
|---|---|---|
| `study-desk/` | Focus tracking | Uses YOLO person / phone detection and unified model calls. |

Scene Packs are clients of the same local HTTP runtime API used by LAN clients. They should not own model-server lifecycle or add private native bridge methods.

Example packaging command from the project root:

```powershell
tar -a -cf dist/scenes/study-desk.scene.zip -C scenes/study-desk manifest.json web/index.html
```

Keep zip entries flat under the scene root. The installer expects `manifest.json` and `web/index.html`, not an extra top-level directory.

---

## 中文

这个目录存放 Lociant Scene Pack 源文件。

Scene Pack 是一个小型、可安装的手机本地体验：

```text
manifest.json
web/index.html
```

当前内置场景：

| Scene | 用途 | 说明 |
|---|---|---|
| `study-desk/` | 专注跟踪 | 使用 YOLO person / phone 检测和统一模型调用。 |

Scene Pack 是本地 HTTP runtime API 的客户端，和局域网客户端使用同一套能力入口。它不应该拥有 model-server 生命周期，也不应该新增私有 native bridge 方法。

从项目根目录打包示例：

```powershell
tar -a -cf dist/scenes/study-desk.scene.zip -C scenes/study-desk manifest.json web/index.html
```

zip 内部路径要保持扁平。安装器期望看到 `manifest.json` 和 `web/index.html`，不要多包一层顶级目录。
