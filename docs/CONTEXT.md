# CONTEXT — Ad Merge

纯 Kotlin Android 融合广告 App。参考 `flutter_merge` 项目中 Android 原生侧的广告架构模式，Compose 构建 UI。

## 技术栈

| 维度 | 选择 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose |
| 架构 | Clean Architecture + 策略模式 |
| 构建 | Gradle Kotlin DSL |
| DI | Koin Annotations |
| 广告 SDK | 友盟 / 穿山甲 / 优量汇 / 百度 (AAR) |
| Activity | 单 Activity + Compose Navigation |
| 最少 SDK | 26 |
| 目标 SDK | 36 |

## 广告位（全量 8 种）

开屏 / 横幅 / 信息流 / 插屏 / 激励视频 / 任务激励 / 纯净激励 / 下载配额激励

## 从参考项目复用的架构模式

参考项目：`E:\project_Android\shili\flutter_merge`（仅取其 Android 原生侧架构，不取其 Flutter 层）

- **AdSdkProvider 策略模式** — 四通道 SDK 可插拔初始化
- **AdSdkManager** — SharedPreferences 缓存通道 + 远程 API 下发 `adType` 动态切换
- **AppAdConfig 动态代码位解析** — 根据当前通道选择对应 SDK 的代码位 ID
- **MergeRemoteConfig** — 远程配置 API URL
- 四个 SDK 的 AAR 包 + 配置常量（AppId / 代码位 ID）

## 不做的东西

- Flutter 层（MethodChannel / EventChannel / PlatformView）
- 手机清理管家
- 参考项目中的 `com.merge.bridge.*` 桥接层（原生之间不需要桥接）

## 领域术语

### 广告通道 (Ad Channel)
四家广告 SDK：友盟联盟(0)、穿山甲/Pangle(1)、优量汇/GDT(2)、百度网盟(3)。远程 API `GET /api/globalSetting/get` 返回 `{ adType: 0|1|2|3 }` 决定当前通道。App 冷启动时先读 SharedPreferences 缓存用上次通道展示开屏，同时拉取远程配置更新缓存。

### 代码位 (Placement ID)
每个广告位（Banner/插屏/激励等）在对应 SDK 中的字符串 ID。每个通道有独立体系，由 `AppAdConfig` 根据当前通道解析。

### AdProvider (策略接口)
每个通道实现此接口：SDK 初始化、广告加载、全屏广告展示、隐私合规。Domain 层只依赖接口，UI 层不感知具体通道。

### 纯净模式 (Pure Mode)
用户完成激励任务后，后端返回 `isPureTaskCompleted = true`，隐藏广告。本地缓存 2 分钟。

### 热启动插屏 (Resume Interstitial)
App 回到前台时展示插屏。最小间隔 600 秒，首次恢复跳过，纯净模式抑制。

---

## 已确认的设计决策

| # | 决策 | 结论 |
|---|---|---|
| 1 | 技术栈 | 纯 Kotlin，不取参考项目的 Flutter 层 |
| 2 | UI | Jetpack Compose |
| 3 | 广告位 | 全量 8 种 |
| 4 | 架构 | Clean Architecture + 策略模式（domain/data/ui） |
| 5 | 远程通道切换 | API 下发 adType → AdChannelStore → AdProvider |
| 6 | Activity | 单 Activity + Compose Navigation |
| 7 | DI | Koin Annotations（编译期验证 + 运行时灵活） |
| 8 | 包结构 | domain/data/ui + data/provider 下按 SDK 物理隔离 |
| 9 | 网络层 | Retrofit + OkHttp |
| 10 | KMP | 不做——广告 SDK 只有 Android AAR，无跨平台场景 |
| 11 | Git 策略 | GitHub Flow：main + feature 分支 |
| 12 | AAR 管理 | 直接提交到 app/libs/，`.gitattributes` 标记 binary |

