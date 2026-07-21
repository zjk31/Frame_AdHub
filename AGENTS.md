# AGENTS.md — Android Harness 总指挥

## 构建命令

```bash
./gradlew assembleDebug    # 编译 debug 包
./gradlew test             # 运行单元测试
./gradlew lint             # 代码规范检查
```

## Kotlin 规范

- 严格遵循 Kotlin 官方编码规范。
- **`!!` 使用**：允许在确定非空的场景（如 `onCreate` 中 `intent.extras!!`），但必须加注释说明原因。回调、网络返回等不确定场景**禁止 `!!`**，必须用 `?.` 或 `?:` 优雅处理。
- 数据类用 `data class`，密封状态用 `sealed class`/`sealed interface`。

## Compose 性能红线

- ✅ **允许** `var xxx by remember { mutableStateOf(...) }` 作为 Composable 本地状态。
- ❌ **严禁** 将 `MutableState` / `SnapshotMutationPolicy` 作为参数传给子 Composable。传参必须用稳定不可变类型（`val`、`data class`、`State<T>` 的 `.value`）。
- ❌ **禁止** 在 `@Composable` 函数内直接调用 `viewModel.someSuspendFun()`。必须通过 `LaunchedEffect` / `rememberCoroutineScope().launch` 包裹。
- 优先使用 `derivedStateOf` 而非在 Composable 内手动计算派生值。
- `LazyColumn` / `LazyRow` 的 `key` 必须稳定唯一，禁止用 index。

## 内存与生命周期

- 注册监听器（如 `LocationListener`、`BroadcastReceiver`、Observer）的位置和取消注册必须成对：
  - `onCreate` → `onDestroy`
  - `onResume` → `onPause`（轻量监听器）
  - `onStart` → `onStop`（前台监听器）
- ❌ 在 `onCreate` 中注册但未在 `onDestroy` 中取消 = **代码不合格**。
- 长生命周期对象（单例、ViewModel）持有 Activity/View 引用必须用 `WeakReference`。
- `DisposableEffect` 的 `onDispose` 必须清理资源。

## 线程与性能

- 网络、数据库、文件 I/O **禁止在主线程执行**。必须切 `Dispatchers.IO` 或使用 Room/Retrofit 内置线程池。
- `onCreate` 启动耗时目标：< 500ms（不含闪屏广告）。
- Compose LazyColumn 滑动帧率必须保持 60fps。出现卡顿先检查：key 是否稳定、item 是否过度重组、是否有主线程阻塞。
- 内存抖动（频繁 GC）：`onDraw` / 高频回调中**禁止创建对象**（`String.format`、boxing、`Collections.singletonList` 等）。

## 文档索引

- 架构设计 → [docs/architecture/mvi.md](docs/architecture/mvi.md)
- 性能排查手册 → [docs/runbooks/performance.md](docs/runbooks/performance.md)
- 项目上下文 → [docs/CONTEXT.md](docs/CONTEXT.md)
- 架构决策 → [docs/decisions/](docs/decisions/)
- 开放问题 → [docs/issues/](docs/issues/)

## Harness 自动检查

```bash
# Windows
.harness\scripts\check.bat

# Mac/Linux
.harness/scripts/check.sh
```

每次提交代码前，AI 必须确认 check 脚本通过，Lint 报错不得跳过。
