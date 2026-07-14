package cn.maga.lingdongmanhua.ui.screens.feed

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.AdSdkManager
import cn.maga.lingdongmanhua.core.PureModeManager
import cn.maga.lingdongmanhua.domain.model.AdLoadState
import cn.maga.lingdongmanhua.domain.model.AdPlacement
import cn.maga.lingdongmanhua.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class FeedUiState(
    val channelName: String = "",
    val codeId: String = "",
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val errorMessage: String? = null,
    val pureModeActive: Boolean = false,
)

class FeedViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _feedViews = MutableStateFlow<List<View>>(emptyList())
    val feedViews: StateFlow<List<View>> = _feedViews.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = FeedUiState(
                channelName = adSdkManager.currentChannel.displayName,
                codeId = adConfigRepo.getCodeId(AdPlacement.Feed),
            )
        }
    }

    fun requestFeed() {
        val state = _uiState.value
        if (state.codeId.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "代码位为空，请先配置")
            return
        }

        // 纯净模式拦截：不加载信息流广告
        if (pureModeManager.checkActive()) {
            _uiState.value = state.copy(
                isLoading = false,
                hasLoaded = false,
                pureModeActive = true,
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, hasLoaded = false, errorMessage = null)
        val codeId = state.codeId

        viewModelScope.launch {
            android.util.Log.e("FeedVM", "requestFeed start: codeId=$codeId")
            // 确保 SDK 已就绪（冷启动场景）
            adSdkManager.ensureProviderReady()
            android.util.Log.e("FeedVM", "ensureProviderReady done")
            val provider = adSdkManager.currentProvider
            android.util.Log.e("FeedVM", "provider=${provider.javaClass.simpleName}, calling loadFeed")
            provider.loadFeed(codeId, 3).collect { adState ->
                android.util.Log.e("FeedVM", "Feed state: $adState")
                when (adState) {
                    is AdLoadState.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is AdLoadState.Loaded -> {
                        _feedViews.value = adState.ad
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasLoaded = true,
                            errorMessage = null,
                        )
                    }
                    is AdLoadState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasLoaded = false,
                            errorMessage = adState.message,
                        )
                    }
                    is AdLoadState.Idle -> {}
                }
            }
        }
    }
}
