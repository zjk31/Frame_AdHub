# Android 性能排查手册

## 启动耗时

### 测量

```bash
# 冷启动耗时（API 24+）
adb shell am start -W <package>/<activity> | grep TotalTime

# Application.onCreate 耗时
# 在 AdHubApp.onCreate 首尾加 System.currentTimeMillis() 差值
```

### 目标

| 指标 | 目标 |
|---|---|
| `Application.onCreate` | < 200ms |
| `SplashAdActivity.onCreate` → 首帧 | < 500ms |
| 冷启动 TotalTime | < 1.5s |

### 优化手段

1. `Application.onCreate` 中只做必要初始化，延迟加载用 `lazy` 或后台协程
2. ContentProvider 延迟初始化（App Startup library）
3. 闪屏不阻塞主线程——广告 SDK 异步加载

---

## 帧率（FPS）

### 测量

```bash
# 实时 GPU 渲染柱状图
# 设置 → 开发者选项 → GPU 呈现模式分析 → 在屏幕上显示为条形图

# Profile HWUI rendering（API 31+）
adb shell dumpsys gfxinfo <package> framestats
```

### 目标

- Compose LazyColumn 滑动：**60fps** 稳定，jank < 5%
- 页面切换动画：不掉帧

### 常见卡顿原因

| 原因 | 排查 |
|---|---|
| key 不稳定 | LazyColumn key 用 index → 改用稳定 id |
| 过度重组 | 检查 `derivedStateOf` vs `remember` 的使用 |
| 主线程阻塞 | StrictMode + profiler 查 IO 操作 |
| 图片解码 | Coil 已处理，但大图需要 `size()` 预设 |

---

## 内存

### 测量

```bash
adb shell dumpsys meminfo <package>
```

### 红线

- Activity leak：旋转 10 次后 dump heap，`Activity` 实例数 > 1 即泄漏
- `onDestroy` 后 `Handler` / `Runnable` / 单例持有 Activity 引用 = **不合格**
- 内存抖动：`onDraw` / Compose 高频重组中禁止创建临时对象

### StrictMode（Debug）

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
}
```

---

## ANR

常见触发原因：
- BroadcastReceiver.onReceive 主线程阻塞 > 10s
- Service.onCreate / onStartCommand > 20s
- Input 事件超时 > 5s

排查：
```bash
adb pull /data/anr/traces.txt
```
