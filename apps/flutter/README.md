# Flutter UI

共享 UI 模块：Android（add-to-app 嵌入设备层宿主）与 Linux 桌面共用。
`PlatformService` 按平台选择实现——Android 是混合版（HTTP 核心 + 设备
操作走 MethodChannel），桌面是纯 HTTP，后端地址可用
`--dart-define=LOCIANT_BASE_URL=...` 覆盖。详见 [架构](../../docs/architecture.md)。
