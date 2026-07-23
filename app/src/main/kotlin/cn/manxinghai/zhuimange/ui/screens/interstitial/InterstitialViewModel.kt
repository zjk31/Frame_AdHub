package cn.manxinghai.zhuimange.ui.screens.interstitial

import android.app.Activity
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.core.PureModeManager
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class InterstitialUiState(
    val isLoading: Boolean = false,
    val channelName: String = "",
    val codeId: String = "",
    val finished: Boolean = false,
    val shown: Boolean = false,
    val errorMessage: String? = null,
    val pureModeActive: Boolean = false,
)

class InterstitialViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterstitialUiState())
    val uiState: StateFlow<InterstitialUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = InterstitialUiState(
                channelName = adSdkManager.currentChannel.displayName,
                codeId = adConfigRepo.getCodeId(AdPlacement.Interstitial),
            )
        }
    }

    fun showInterstitial(activity: Activity) {
        val state = _uiState.value
        if (state.codeId.isEmpty()) {
            _uiState.value = state.copy(
                finished = true,
                errorMessage = "代码位为空，请先配置广告位",
            )
            return
        }

        // 纯净模式拦截：不展示插屏
        if (pureModeManager.checkActive()) {
            _uiState.value = state.copy(
                finished = true,
                shown = false,
                pureModeActive = true,
                errorMessage = "纯净模式已激活，广告已屏蔽",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            adSdkManager.ensureProviderReady()
            val provider = adSdkManager.currentProvider
            val result = provider.showInterstitial(
                activity = activity,
                codeId = state.codeId,
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                finished = true,
                shown = result,
                errorMessage = if (result) null else "插屏展示失败或用户未观看",
            )
        }
    }

    fun reset() {
        _uiState.value = InterstitialUiState()
    }
}
