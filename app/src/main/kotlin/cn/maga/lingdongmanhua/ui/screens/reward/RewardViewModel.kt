package cn.maga.lingdongmanhua.ui.screens.reward

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.AdSdkManager
import cn.maga.lingdongmanhua.core.PureModeManager
import cn.maga.lingdongmanhua.domain.model.AdPlacement
import cn.maga.lingdongmanhua.domain.model.RewardResult
import cn.maga.lingdongmanhua.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 激励视频页面状态。
 */
data class RewardUiState(
    val isLoading: Boolean = true,
    val channelName: String = "",
    val codeId: String = "",
    val slotKey: String = "",
    val result: RewardResult? = null,
    val errorMessage: String? = null,
)

class RewardViewModel(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: PureModeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardUiState())
    val uiState: StateFlow<RewardUiState> = _uiState.asStateFlow()

    fun loadReward(slotKey: String) {
        _uiState.value = RewardUiState(
            isLoading = true,
            slotKey = slotKey,
            channelName = adSdkManager.currentChannel.displayName,
            codeId = "",  // 异步加载
        )
        viewModelScope.launch {
            val codeId = resolveCodeId(slotKey)
            _uiState.value = _uiState.value.copy(codeId = codeId, isLoading = false)
        }
    }

    fun startReward(activity: Activity) {
        val state = _uiState.value
        if (state.codeId.isEmpty()) {
            _uiState.value = state.copy(
                isLoading = false,
                errorMessage = "代码位为空，请先配置广告位",
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            adSdkManager.ensureProviderReady()
            val provider = adSdkManager.currentProvider
            val result = provider.showRewardVideo(
                activity = activity,
                codeId = state.codeId,
                slotKey = state.slotKey,
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = result,
                errorMessage = result.errorMessage,
            )

            // 纯净激励完成后，授予免广告权益
            if (state.slotKey == "pureReward" && result.rewardGranted) {
                pureModeManager.grantReward()
            }
        }
    }

    fun reset() {
        _uiState.value = RewardUiState()
    }

    private suspend fun resolveCodeId(slotKey: String): String {
        val placement = when (slotKey) {
            "taskReward" -> AdPlacement.TaskReward
            "pureReward" -> AdPlacement.PureReward
            "downloadQuotaReward" -> AdPlacement.DownloadQuotaReward
            else -> AdPlacement.Reward
        }
        return adConfigRepo.getCodeId(placement)
    }

    companion object {
        private const val TAG = "RewardViewModel"
    }
}
