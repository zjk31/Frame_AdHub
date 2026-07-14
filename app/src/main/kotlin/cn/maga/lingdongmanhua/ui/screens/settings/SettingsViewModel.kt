package cn.maga.lingdongmanhua.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.maga.lingdongmanhua.data.download.DownloadManager
import cn.maga.lingdongmanhua.data.repository.UserSession
import cn.maga.lingdongmanhua.data.settings.GlobalSettingsManager
import cn.maga.lingdongmanhua.data.sse.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val adEnabled: Boolean = true,
    val autoDownload: Boolean = false,
    val downloadWifiOnly: Boolean = true,
    val nightModeDefault: Boolean = false,
    val autoScrollInterval: Int = 5,
    val sseConnected: Boolean = false,
    val downloadSizeBytes: Long = 0L,
    val cacheCleared: Boolean = false
)

class SettingsViewModel(
    private val globalSettingsManager: GlobalSettingsManager,
    private val downloadManager: DownloadManager,
    private val messageRepository: MessageRepository,
    private val userSession: UserSession
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        loadSettings()
        observeSseState()
    }

    private fun loadSettings() {
        _ui.update {
            it.copy(
                adEnabled = globalSettingsManager.isAdEnabled(),
                autoDownload = globalSettingsManager.isAutoDownloadEnabled(),
                downloadWifiOnly = globalSettingsManager.isDownloadWifiOnly(),
                nightModeDefault = globalSettingsManager.isNightModeDefault(),
                autoScrollInterval = globalSettingsManager.getAutoScrollInterval(),
                downloadSizeBytes = downloadManager.getDownloadSize()
            )
        }
    }

    private fun observeSseState() {
        viewModelScope.launch {
            messageRepository.connectionState.collect { state ->
                _ui.update { it.copy(sseConnected = state == cn.maga.lingdongmanhua.data.sse.SseConnectionState.CONNECTED) }
            }
        }
    }

    fun setAdEnabled(enabled: Boolean) {
        globalSettingsManager.setAdEnabled(enabled)
        _ui.update { it.copy(adEnabled = enabled) }
    }

    fun setAutoDownload(enabled: Boolean) {
        globalSettingsManager.setAutoDownload(enabled)
        _ui.update { it.copy(autoDownload = enabled) }
    }

    fun setDownloadWifiOnly(wifiOnly: Boolean) {
        globalSettingsManager.setDownloadWifiOnly(wifiOnly)
        _ui.update { it.copy(downloadWifiOnly = wifiOnly) }
    }

    fun setNightModeDefault(enabled: Boolean) {
        globalSettingsManager.setNightModeDefault(enabled)
        _ui.update { it.copy(nightModeDefault = enabled) }
    }

    fun setAutoScrollInterval(seconds: Int) {
        globalSettingsManager.setAutoScrollInterval(seconds)
        _ui.update { it.copy(autoScrollInterval = seconds) }
    }

    fun clearDownloadCache() {
        viewModelScope.launch {
            downloadManager.clearAllDownloads()
            _ui.update {
                it.copy(
                    downloadSizeBytes = 0L,
                    cacheCleared = true
                )
            }
        }
    }

    fun reconnectSse() {
        messageRepository.reconnect()
    }

    fun disconnectSse() {
        messageRepository.disconnect()
    }
}
