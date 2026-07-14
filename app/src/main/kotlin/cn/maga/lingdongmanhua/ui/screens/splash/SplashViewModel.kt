package cn.maga.lingdongmanhua.ui.screens.splash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.AdSdkManager
import cn.maga.lingdongmanhua.core.PureModeManager
import cn.maga.lingdongmanhua.domain.model.AdPlacement
import cn.maga.lingdongmanhua.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplashUiState(
    val channelName: String = "",
    val codeId: String = "",
    val statusText: String = "正在初始化 SDK…",
    val isShowingAd: Boolean = false,     // true=广告正在展示（隐藏 Loading UI）
    val resolved: Boolean = false,         // true=流程结束，可跳首页
)

class SplashViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /**
     * 由 [MainActivity.setKeepOnScreenCondition] 调用。
     * 返回 true 表示系统启动页可以淡出：广告已就绪 或 流程结束无需广告。
     */
    fun canDismissSplash(): Boolean {
        val s = _uiState.value
        return s.isShowingAd || s.resolved
    }

    /**
     * 冷启动流程：显示品牌页 → 初始化 SDK → 展示开屏广告 → 标记完成。
     * 由 SplashScreen 通过 LaunchedEffect 调用，不依赖用户点击。
     */
    fun startColdStartFlow(activity: Activity) {
        if (_uiState.value.resolved) return

        viewModelScope.launch {
            // 1. 获取配置
            val channel = adSdkManager.currentChannel
            val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
            _uiState.value = _uiState.value.copy(
                channelName = channel.displayName,
                codeId = codeId,
                statusText = "正在初始化 SDK…",
            )

            // 2. 纯净模式检测
            if (pureModeManager.checkActive()) {
                _uiState.value = _uiState.value.copy(
                    statusText = "纯净模式已激活，跳过开屏广告",
                    resolved = true,
                )
                return@launch
            }

            // 3. 初始化 SDK
            _uiState.value = _uiState.value.copy(statusText = "正在初始化 SDK…")
            try {
                adSdkManager.ensureProviderReady()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusText = "SDK 初始化失败: ${e.message}",
                    resolved = true,
                )
                return@launch
            }

            // 4. 展示开屏广告
            _uiState.value = _uiState.value.copy(
                statusText = "正在加载开屏广告…",
                isShowingAd = true,  // 隐藏 Loading UI，让广告层显示
            )

            try {
                val provider = adSdkManager.currentProvider
                val container = android.widget.FrameLayout(activity)
                provider.showSplashAd(activity, codeId, container, null, null)
                // showSplashAd 是 suspend，返回即广告关闭
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusText = "开屏广告异常: ${e.message}",
                )
            }

            // 5. 无论成功/失败/异常，都进入首页
            _uiState.value = _uiState.value.copy(
                isShowingAd = false,
                resolved = true,
            )
        }
    }
}
