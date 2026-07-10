package cn.android.adhub.ui.screens.banner

import android.app.Activity
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.core.PureModeManager
import cn.android.adhub.domain.model.AdLoadState
import cn.android.adhub.domain.model.AdPlacement
import cn.android.adhub.domain.repository.AdConfigRepository
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BannerUiState(
    val adState: AdLoadState<View> = AdLoadState.Loading,
    val channelName: String = "",
    val codeId: String = "",
    val pureModeActive: Boolean = false,
)

class BannerViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BannerUiState())
    val uiState: StateFlow<BannerUiState> = _uiState.asStateFlow()

    /** 由 BannerScreen 通过 LocalContext 注入，GDT/百度 Banner 需要 Activity Context */
    @Volatile
    private var boundActivity: Activity? = null

    fun setActivity(activity: Activity?) {
        boundActivity = activity
    }

    init {
        loadBanner()
    }

    fun loadBanner() {
        // 纯净模式拦截：不加载广告
        if (pureModeManager.checkActive()) {
            _uiState.value = BannerUiState(
                adState = AdLoadState.Idle,
                channelName = adSdkManager.currentChannel.displayName,
                codeId = "",
                pureModeActive = true,
            )
            return
        }

        viewModelScope.launch {
            // 确保 SDK 就绪后再加载广告（首次冷启动可能需要等待）
            adSdkManager.ensureProviderReady()
            val channel = adSdkManager.currentChannel
            val provider = adSdkManager.currentProvider
            val codeId = adConfigRepo.getCodeId(AdPlacement.Banner)

            _uiState.value = BannerUiState(
                adState = AdLoadState.Loading,
                channelName = channel.displayName,
                codeId = codeId,
                pureModeActive = false,
            )

            // 带重试的广告加载：首冷启动 GroMore 配置下载需要时间
            loadWithRetry(provider, codeId, 3)
        }
    }

    /** 带重试的广告加载，首失败等待 3s×指数退避，应对 840040 等冷启动时序问题。 */
    private suspend fun loadWithRetry(
        provider: cn.android.adhub.domain.provider.AdProvider,
        codeId: String,
        maxRetries: Int,
    ) {
        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val waitMs = 3000L * attempt
                Log.w("BannerVM", "Banner load retry $attempt/$maxRetries after ${waitMs}ms")
                _uiState.value = _uiState.value.copy(adState = AdLoadState.Loading)
                delay(waitMs)
            }
            // 用 first 而不是 collect：StateFlow 是无限流，collect 会永远阻塞
            val result = provider.loadBanner(codeId, boundActivity).first { it !is AdLoadState.Loading }
            _uiState.value = _uiState.value.copy(adState = result)
            if (result is AdLoadState.Loaded) break
        }
    }
}
