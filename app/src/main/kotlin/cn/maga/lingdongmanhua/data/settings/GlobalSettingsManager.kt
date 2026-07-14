package cn.maga.lingdongmanhua.data.settings

import cn.maga.lingdongmanhua.data.api.ManAppApi
import cn.maga.lingdongmanhua.data.dto.GlobalSettings
import cn.maga.lingdongmanhua.domain.model.BannerManga
import cn.maga.lingdongmanhua.domain.model.Category
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 全局设置管理器
 * 管理广告开关、分类列表、Banner 等
 */
class GlobalSettingsManager(
    private val repository: ManAppRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(GlobalSettingsData())
    val settings: StateFlow<GlobalSettingsData> = _settings.asStateFlow()

    private val _banners = MutableStateFlow<List<BannerManga>>(emptyList())
    val banners: StateFlow<List<BannerManga>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        scope.launch {
            // 加载全局设置
            repository.getGlobalCategories().onSuccess { cats ->
                _categories.value = cats
                _settings.update { it.copy(categoriesLoaded = true) }
            }

            // 加载 Banner
            repository.getHomeBanners().onSuccess { banners ->
                _banners.value = banners
            }
        }
    }

    /**
     * 广告是否启用
     */
    fun isAdEnabled(): Boolean = _settings.value.adEnabled

    /**
     * 设置广告开关（本地覆盖，不持久化）
     */
    fun setAdEnabled(enabled: Boolean) {
        _settings.update { it.copy(adEnabled = enabled) }
    }

    fun isAutoDownloadEnabled(): Boolean = _settings.value.autoDownload
    fun setAutoDownload(enabled: Boolean) {
        _settings.update { it.copy(autoDownload = enabled) }
    }

    fun isDownloadWifiOnly(): Boolean = _settings.value.downloadWifiOnly
    fun setDownloadWifiOnly(wifiOnly: Boolean) {
        _settings.update { it.copy(downloadWifiOnly = wifiOnly) }
    }

    fun isNightModeDefault(): Boolean = _settings.value.nightModeDefault
    fun setNightModeDefault(enabled: Boolean) {
        _settings.update { it.copy(nightModeDefault = enabled) }
    }

    fun getAutoScrollInterval(): Int = _settings.value.autoScrollInterval
    fun setAutoScrollInterval(seconds: Int) {
        _settings.update { it.copy(autoScrollInterval = seconds) }
    }
}

/**
 * 全局设置数据
 */
data class GlobalSettingsData(
    val adEnabled: Boolean = true,
    val autoDownload: Boolean = false,
    val downloadWifiOnly: Boolean = true,
    val nightModeDefault: Boolean = false,
    val autoScrollInterval: Int = 5,
    val categoriesLoaded: Boolean = false
)
