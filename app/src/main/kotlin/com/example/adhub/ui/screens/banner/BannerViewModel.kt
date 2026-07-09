package com.example.adhub.ui.screens.banner

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adhub.data.AdSdkManager
import com.example.adhub.core.PureModeManager
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.repository.AdConfigRepository
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        provider: com.example.adhub.domain.provider.AdProvider,
        codeId: String,
        maxRetries: Int,
    ) {
        var lastError: AdLoadState.Error? = null
        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val waitMs = 3000L * attempt
                Log.w("BannerVM", "Banner load retry $attempt/$maxRetries after ${waitMs}ms")
                _uiState.value = _uiState.value.copy(adState = AdLoadState.Loading)
                delay(waitMs)
            }
            var done = false
            provider.loadBanner(codeId).collect { state ->
                if (state is AdLoadState.Error) {
                    lastError = state
                } else if (state is AdLoadState.Loaded) {
                    done = true
                }
                _uiState.value = _uiState.value.copy(adState = state)
            }
            if (done) break
        }
        // 如果全部重试都失败，最终状态保留最后错误
        if (lastError != null && _uiState.value.adState is AdLoadState.Error) {
            // 已显示，无需额外处理
        }
    }
}
