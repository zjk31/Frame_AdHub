# MVI 架构（Model-View-Intent）

## 核心理念

单向数据流（UDF），State 不可变，View 只消费 State，不直接修改。

```
Intent  →  ViewModel  →  Model（Repository/DataSource）
   ↑                        ↓
   └── State（StateFlow） ← ─┘
        ↓
      View（Compose UI）
```

## 约定

- **State**：`data class`，`val` 字段全部不可变。通过 `copy()` 生成新状态。
- **Event/SideEffect**：`sealed class`，一次性事件（Toast、导航）走 `Channel` / `SharedFlow`，不走 `StateFlow`。
- **ViewModel**：仅持有 `StateFlow<UiState>` + `suspend fun onEvent(event: UiEvent)`。
- **View**：`@Composable fun Screen(state: UiState, onEvent: (UiEvent) -> Unit)`。

## 示例骨架

```kotlin
// UiState.kt
data class HomeUiState(
    val items: List<String> = emptyList(),
    val isLoading: Boolean = false,
)

// UiEvent.kt
sealed class HomeUiEvent {
    data class OnItemClick(val id: String) : HomeUiEvent()
    data object OnRefresh : HomeUiEvent()
}

// HomeViewModel.kt
class HomeViewModel(private val repo: HomeRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onEvent(event: HomeUiEvent) { /* ... */ }
}

// HomeScreen.kt
@Composable
fun HomeScreen(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit) {
    LazyColumn { /* ... */ }
}
```

## 禁止反模式

- ❌ View 直接调用 `viewModel.someMethod()` 修改 State。
- ❌ State 中使用 `MutableList` / `var` 可变字段。
- ❌ ViewModel 持有 Context / View 引用。
