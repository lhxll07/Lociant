# Future Direction

> Version: 0.4.1 | Updated: 2026-05-15

[English](#english) · [中文](#中文)

## English

MNNode is moving from "an app that runs scenes" to "a phone-sized local AI runtime node." The next work should reduce architectural weight while making the runtime usable without keeping the UI open.

## Direction

Best-fit uses are local, unattended, and event-driven:

- local model API node for LAN clients
- baby / person guard
- elder safety
- pet watch
- store / room guard

`study-desk` remains a useful prototype. `model-server` and caretaker scenes are closer to the product core.

## Near-Term Plan

1. **Visible Runtime Node**
   - foreground service owns the model-server lifecycle
   - Runtime Window shows status, model, and LAN URL over other apps
   - click opens the app, long press hides, double click toggles runtime
   - boot autostart stays out of scope and opt-in only later

2. **Runtime Core Cleanup**
   - keep `MNNodeRuntime` as the shared ownership boundary
   - continue shrinking `MainActivity`
   - avoid duplicate controllers and legacy model paths

3. **Runtime Event Store**
   - persist scene runtime events, alerts, API requests, and lifecycle events in Room
   - make runtime behavior inspectable after scene switching or UI close

4. **Model Capabilities**
   - expose text/image/stream/context/cache capability metadata
   - use capabilities in `/v1/models`, model switching UI, and scene checks

5. **LAN Hardening**
   - optional auth token
   - clearer LAN URL/status
   - request diagnostics and recent errors

6. **System Panel Positioning**
   - treat `model-server` as runtime infrastructure
   - keep Scene Packs as app-level experiences
   - do not let third-party scenes own server lifecycle

## Out Of Scope For Now

- pure headless camera / vision
- invisible long-running daemon mode
- default boot autostart
- public internet exposure
- multi-node scheduling
- complex plugin marketplace

These are valid later goals, but they would make the personal project too heavy before the runtime core is stable.

## Principle

Prefer one runtime path over compatibility layers. Delete old paths when the new path works. Add features only when they strengthen the phone-as-runtime-node direction.

---

## 中文

MNNode 正在从“运行场景的 App”走向“手机大小的本地 AI runtime 节点”。下一阶段应该降低架构重量，同时让 runtime 不依赖 UI 常驻也能工作。

## 方向

最适合的任务是本地、无人值守、事件驱动：

- 面向局域网客户端的本地模型 API 节点
- 婴儿 / 人员看护
- 老人安全
- 宠物看护
- 店铺 / 房间看守

`study-desk` 仍然是有价值的原型。`model-server` 和看护类场景更接近产品核心。

## 近期计划

1. **Visible Runtime Node**
   - 前台服务持有 model-server 生命周期
   - Runtime Window 在其他 App 上方显示状态、模型和 LAN URL
   - 点击打开 App，长按隐藏，双击启停 runtime
   - 开机自启暂不做，以后也只作为 opt-in

2. **Runtime Core 清理**
   - 保持 `MNNodeRuntime` 作为共享所有权边界
   - 继续瘦身 `MainActivity`
   - 避免重复 controller 和旧模型路径

3. **Runtime Event Store**
   - 用 Room 持久化场景 runtime 事件、告警、API 请求和生命周期事件
   - UI 关闭或切换场景后仍可查看 runtime 行为

4. **模型能力协议**
   - 暴露 text/image/stream/context/cache 等能力元数据
   - 用于 `/v1/models`、模型切换 UI 和场景检查

5. **局域网加固**
   - 可选 auth token
   - 更清晰的 LAN URL / 状态
   - 请求诊断和最近错误

6. **系统面板定位**
   - 把 `model-server` 当作 runtime 基础设施
   - Scene Pack 保持应用层体验
   - 第三方场景不拥有 server 生命周期

## 暂不做

- 纯 headless camera / vision
- 隐藏式长期后台 daemon
- 默认开机自启
- 公网暴露
- 多节点调度
- 复杂插件市场

这些方向以后可以做，但在 runtime core 稳定前会让个人项目过重。

## 原则

优先保留一条 runtime 主路径，而不是堆兼容层。新路径跑通后删除旧路径。新增功能必须强化“手机作为 runtime 节点”这个方向。
