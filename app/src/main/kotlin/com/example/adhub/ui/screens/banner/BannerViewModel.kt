package com.example.adhub.ui.screens.banner

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adhub.data.AdSdkManager
import com.example.adhub.core.PureModeManager
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.repository.AdConfigRepository
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
            val channel = adSdkManager.currentChannel
            val provider = adSdkManager.currentProvider
            val codeId = adConfigRepo.getCodeId(AdPlacement.Banner)

            _uiState.value = BannerUiState(
                adState = AdLoadState.Loading,
                channelName = channel.displayName,
                codeId = codeId,
                pureModeActive = false,
            )

            provider.loadBanner(codeId).collect { adState ->
                _uiState.value = _uiState.value.copy(adState = adState)
            }
        }
    }
}
