package com.example.adhub.ui.screens.splash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adhub.data.AdSdkManager
import com.example.adhub.core.PureModeManager
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplashUiState(
    val channelName: String = "",
    val codeId: String = "",
    val isShowing: Boolean = false,
    val result: SplashResult? = null,
    val pureModeActive: Boolean = false,
)

sealed class SplashResult {
    data object Success : SplashResult()
    data class Failed(val message: String) : SplashResult()
    data object Skipped : SplashResult()  // 纯净模式跳过
}

class SplashViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        val channel = adSdkManager.currentChannel
        viewModelScope.launch {
            val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
            _uiState.value = SplashUiState(
                channelName = channel.displayName,
                codeId = codeId,
            )
        }
    }

    fun showSplash(activity: Activity) {
        if (_uiState.value.isShowing) return

        // 纯净模式拦截：不展示开屏广告
        if (pureModeManager.checkActive()) {
            _uiState.value = _uiState.value.copy(
                isShowing = false,
                result = SplashResult.Skipped,
                pureModeActive = true,
            )
            return
        }

        viewModelScope.launch {
            val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
            val provider = adSdkManager.currentProvider

            _uiState.value = _uiState.value.copy(
                isShowing = true,
                result = null,
            )

            try {
                val success = provider.showSplashAd(activity, codeId)
                _uiState.value = _uiState.value.copy(
                    isShowing = false,
                    result = if (success) SplashResult.Success
                    else SplashResult.Failed("广告展示失败"),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isShowing = false,
                    result = SplashResult.Failed(e.message ?: "未知错误"),
                )
            }
        }
    }
}
