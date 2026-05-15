# Scene Pack Guide

> Version: 0.4.1 | Updated: 2026-05-15

[English](#english) · [中文](#中文)

## English

A Scene Pack is an app-level experience mounted inside MNNode's WebView shell. It may use native capabilities, but long-running state should live in the runtime.

## Structure

```text
my-scene/
  manifest.json
  web/
    index.html
```

Installable zip contents must contain `manifest.json` and `web/index.html` directly.

```powershell
tar -a -cf dist/scenes/my-scene.scene.zip -C scenes/my-scene manifest.json web/index.html
```

## Manifest

```json
{
  "id": "my-scene",
  "name": "My Scene",
  "version": "0.1.0",
  "description": "A local sensing scene.",
  "entry": "web/index.html",
  "permissions": ["camera"],
  "capabilities": ["camera-preview", "vision-analysis"]
}
```

Common fields:

| Field | Required | Meaning |
|---|---|---|
| `id` | Yes | Stable scene id. |
| `name` | Yes | Display name. |
| `version` | Yes | Scene version. |
| `entry` | Yes | Web entry path. |
| `permissions` | No | Common value: `camera`. |
| `capabilities` | No | Native capabilities used by the scene. |
| `models` | No | Model requirements. |
| `runtime` | No | Native runtime rules. |

`system` and `kind` are reserved for built-in panels such as `model-server`.

## Capabilities

| Capability | Meaning |
|---|---|
| `camera-preview` | Uses camera preview. |
| `vision-analysis` | Uses object detection frames. |
| `vision-settings` | Has scene-side vision settings. |
| `runtime-rules` | Uses native JSON rule runtime. |
| `model-chat` | Uses unified text/image model calls. |

## Native Runtime Rules

Use `vision-rules` when a scene must keep state after the iframe is closed.

```json
"runtime": {
  "type": "vision-rules",
  "state": { "initial": "watch", "states": ["watch", "alert"] },
  "rules": [
    {
      "id": "person-missing",
      "conditions": [
        { "missing": "person", "classId": 0, "confidenceGte": 0.45 }
      ],
      "forMs": 5000,
      "cooldownKey": "guard-alert",
      "cooldownMs": 12000,
      "actions": [
        { "type": "setState", "state": "alert" },
        { "type": "alert", "level": "critical", "title": "Person missing" },
        { "type": "emit", "name": "guard.person.missing" }
      ]
    }
  ]
}
```

Rule notes:

- `conditions` are AND.
- `present` checks that a detection exists.
- `missing` checks that a detection does not exist.
- `classId` is recommended for COCO classes.
- `forMs` and `cooldownKey` prevent noisy alerts.

## Messages

Scene to shell:

| Type | Purpose |
|---|---|
| `scene.ready` | Scene UI loaded. |
| `vision.start` / `vision.stop` | Start or stop detection. |
| `runtime.subscribe` | Request current runtime snapshot. |
| `runtime.command` | Start, pause, reset, stop, sync runtime. |
| `model.chat` | Call the unified text/image model layer. |
| `api.server.*` | Reserved for the built-in `model-server` panel. |

Shell to scene:

| Type | Purpose |
|---|---|
| `runtime.snapshot` | Full runtime state. |
| `runtime.state` | Runtime state changed. |
| `runtime.event` | Runtime emitted an event. |
| `runtime.locale` | Current language. |
| `scene.dispose` | Iframe is unloading. |
| `model.chat.result` | Model call result. |

Minimal scene startup:

```javascript
function post(message) {
  if (parent && parent !== window) parent.postMessage(message, '*')
}

window.addEventListener('DOMContentLoaded', () => {
  post({ type: 'scene.ready', sceneId: 'my-scene' })
  post({ type: 'runtime.subscribe', sceneId: 'my-scene' })
})
```

## Model Calls

Scenes should use `model.chat`, not private model bridges.

```javascript
post({
  type: 'model.chat',
  sceneId: 'my-scene',
  requestId: 'req-' + Date.now(),
  model: 'qwen3.5-2b-mnn',
  sessionId: 'scene/my-scene/default',
  max_tokens: 128,
  messages: [
    {
      role: 'user',
      content: [
        { type: 'text', text: 'What is in this image?' },
        { type: 'image_url', image_url: { url: 'data:image/jpeg;base64,...' } }
      ]
    }
  ]
})
```

## Authoring Rules

- Keep scene ids stable.
- Declare only capabilities actually used.
- Put long-running decisions in native runtime rules.
- Treat runtime snapshots as the source of truth.
- Keep VLM calls low-frequency.
- Clean UI-only resources on `scene.dispose`.
- Do not let third-party scenes own model-server lifecycle.

Reference scenes:

- `baby-guard`: clean runtime-rule example.
- `study-desk`: camera, vision settings, and model chat prototype.
- `model-server`: built-in runtime panel, not a third-party scene.

---

## 中文

Scene Pack 是挂载在 MNNode WebView shell 里的应用层体验。它可以使用 native 能力，但长期状态应该属于 runtime。

## 结构

```text
my-scene/
  manifest.json
  web/
    index.html
```

可安装 zip 必须直接包含 `manifest.json` 和 `web/index.html`。

```powershell
tar -a -cf dist/scenes/my-scene.scene.zip -C scenes/my-scene manifest.json web/index.html
```

## Manifest

```json
{
  "id": "my-scene",
  "name": "My Scene",
  "version": "0.1.0",
  "description": "A local sensing scene.",
  "entry": "web/index.html",
  "permissions": ["camera"],
  "capabilities": ["camera-preview", "vision-analysis"]
}
```

常用字段：

| 字段 | 必填 | 含义 |
|---|---|---|
| `id` | 是 | 稳定场景 id。 |
| `name` | 是 | 展示名称。 |
| `version` | 是 | 场景版本。 |
| `entry` | 是 | Web 入口路径。 |
| `permissions` | 否 | 常见值：`camera`。 |
| `capabilities` | 否 | 场景使用的 native 能力。 |
| `models` | 否 | 模型需求。 |
| `runtime` | 否 | Native runtime 规则。 |

`system` 和 `kind` 保留给 `model-server` 这类内置面板。

## Capabilities

| 能力 | 含义 |
|---|---|
| `camera-preview` | 使用摄像头预览。 |
| `vision-analysis` | 使用目标检测帧。 |
| `vision-settings` | 有场景侧视觉设置。 |
| `runtime-rules` | 使用 native JSON 规则 runtime。 |
| `model-chat` | 使用统一文本/图片模型调用。 |

## Native Runtime 规则

如果场景需要在 iframe 关闭后仍保持状态，使用 `vision-rules`。

```json
"runtime": {
  "type": "vision-rules",
  "state": { "initial": "watch", "states": ["watch", "alert"] },
  "rules": [
    {
      "id": "person-missing",
      "conditions": [
        { "missing": "person", "classId": 0, "confidenceGte": 0.45 }
      ],
      "forMs": 5000,
      "cooldownKey": "guard-alert",
      "cooldownMs": 12000,
      "actions": [
        { "type": "setState", "state": "alert" },
        { "type": "alert", "level": "critical", "title": "Person missing" },
        { "type": "emit", "name": "guard.person.missing" }
      ]
    }
  ]
}
```

规则说明：

- `conditions` 是 AND。
- `present` 检查检测是否存在。
- `missing` 检查检测是否不存在。
- COCO 类建议填写 `classId`。
- `forMs` 和 `cooldownKey` 用于抑制噪声告警。

## Messages

Scene 到 shell：

| 类型 | 用途 |
|---|---|
| `scene.ready` | 场景 UI 已加载。 |
| `vision.start` / `vision.stop` | 启停检测。 |
| `runtime.subscribe` | 请求当前 runtime 快照。 |
| `runtime.command` | 启动、暂停、重置、停止、同步 runtime。 |
| `model.chat` | 调用统一文本/图片模型层。 |
| `api.server.*` | 保留给内置 `model-server` 面板。 |

Shell 到 scene：

| 类型 | 用途 |
|---|---|
| `runtime.snapshot` | 完整 runtime 状态。 |
| `runtime.state` | runtime 状态变化。 |
| `runtime.event` | runtime 发出事件。 |
| `runtime.locale` | 当前语言。 |
| `scene.dispose` | iframe 即将卸载。 |
| `model.chat.result` | 模型调用结果。 |

最小启动代码：

```javascript
function post(message) {
  if (parent && parent !== window) parent.postMessage(message, '*')
}

window.addEventListener('DOMContentLoaded', () => {
  post({ type: 'scene.ready', sceneId: 'my-scene' })
  post({ type: 'runtime.subscribe', sceneId: 'my-scene' })
})
```

## 模型调用

场景应该使用 `model.chat`，不要新增私有模型桥。

```javascript
post({
  type: 'model.chat',
  sceneId: 'my-scene',
  requestId: 'req-' + Date.now(),
  model: 'qwen3.5-2b-mnn',
  sessionId: 'scene/my-scene/default',
  max_tokens: 128,
  messages: [
    {
      role: 'user',
      content: [
        { type: 'text', text: 'What is in this image?' },
        { type: 'image_url', image_url: { url: 'data:image/jpeg;base64,...' } }
      ]
    }
  ]
})
```

## 编写规则

- 保持场景 id 稳定。
- 只声明实际使用的能力。
- 长期决策放进 native runtime rules。
- 把 runtime snapshot 当作事实源。
- VLM 调用保持低频。
- 在 `scene.dispose` 时清理 UI-only 资源。
- 第三方场景不拥有 model-server 生命周期。

参考场景：

- `baby-guard`：清晰的 runtime-rule 示例。
- `study-desk`：摄像头、视觉设置和模型对话原型。
- `model-server`：内置 runtime 面板，不是第三方场景。
