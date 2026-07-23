package cn.manxinghai.zhuimange.data.repository

import android.content.Context
import android.content.SharedPreferences
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.data.remote.RemoteConfigData
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AdConfigRepository] 本地实现。
 *
 * 仅读 SharedPreferences 缓存，不做网络请求。
 * 子 App 负责在合适的时机调用自己的后端，将配置写入 SharedPreferences（与骨架共用 PREFS_NAME）。
 */
class AdConfigRepositoryImpl(
    private val adSdkManager: AdSdkManager,
) : AdConfigRepository {

    private val _configVersion = MutableStateFlow(0)
    override val configVersion: StateFlow<Int> = _configVersion.asStateFlow()

    private val prefs: SharedPreferences
        get() = AppContextHolder.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun fetchRemoteChannel(): Result<AdChannel> {
        return adSdkManager.refreshLocalChannel()
    }

    override suspend fun fetchAdConfig(): RemoteConfigData? {
        val data = readCachedConfig()
        if (data != null) {
            data.adType?.let { adSdkManager.applyRemoteAdType(it) }
            _configVersion.value++
        }
        return data
    }

    // ── 本地缓存读写 ──

    private fun readCachedConfig(): RemoteConfigData? {
        if (!prefs.contains(KEY_SPLASH)) return null
        return RemoteConfigData(
            adType = prefs.getInt(KEY_AD_TYPE, -1).takeIf { it >= 0 },
            adStatus = prefs.getInt(KEY_AD_STATUS, 1),
            adBannerStatus = prefs.getInt(KEY_AD_BANNER_STATUS, 1),
            adIntersStatus = prefs.getInt(KEY_AD_INTERS_STATUS, 1),
            adRewardStatus = prefs.getInt(KEY_AD_REWARD_STATUS, 1),
            adSplashStatus = prefs.getInt(KEY_AD_SPLASH_STATUS, 1),
            adFreeStatus = prefs.getInt(KEY_AD_FREE_STATUS, 1),
            adSplashCode = prefs.getString(KEY_SPLASH, null),
            adBannerCode = prefs.getString(KEY_BANNER, null),
            adMiniBannerCode = prefs.getString(KEY_BANNER_MINI, null),
            adRewardCode = prefs.getString(KEY_REWARD, null),
            adTaskCode = prefs.getString(KEY_TASK, null),
            adPureCode = prefs.getString(KEY_PURE, null),
            adDownloadCode = prefs.getString(KEY_DOWNLOAD, null),
            adInterstitialCode = prefs.getString(KEY_INTERSTITIAL, null),
            adFreeCode = prefs.getString(KEY_FREE, null),
        )
    }

    override suspend fun getCodeId(placement: AdPlacement): String {
        val remote = readCachedConfig()
        if (remote?.adStatus == 0) return ""
        if (remote != null && !isAdTypeEnabled(remote, placement)) return ""
        val channel = adSdkManager.currentChannel
        val effectiveRemote = remote?.takeIf { it.adType == channel.code }
        return AdCodeIdResolver.resolve(channel, placement, effectiveRemote)
    }

    private fun isAdTypeEnabled(remote: RemoteConfigData, placement: AdPlacement): Boolean = when (placement) {
        AdPlacement.Banner, AdPlacement.BannerMini -> remote.adBannerStatus != 0
        AdPlacement.Interstitial -> remote.adIntersStatus != 0
        AdPlacement.Reward, AdPlacement.TaskReward, AdPlacement.PureReward, AdPlacement.DownloadQuotaReward -> remote.adRewardStatus != 0
        AdPlacement.Splash -> remote.adSplashStatus != 0
        AdPlacement.Feed -> remote.adFreeStatus != 0
    }

    override fun isAdTypeEnabled(placement: AdPlacement): Boolean {
        val remote = readCachedConfig() ?: return true
        if (remote.adStatus == 0) return false
        return isAdTypeEnabled(remote, placement)
    }

    override suspend fun isPureMode(): Boolean = false

    companion object {
        /** 与 SplashAdActivity 共用 prefs 名 */
        const val PREFS_NAME = "ad_remote_config"
        private const val KEY_AD_TYPE = "ad_type"
        private const val KEY_AD_STATUS = "ad_status"
        private const val KEY_AD_BANNER_STATUS = "ad_banner_status"
        private const val KEY_AD_INTERS_STATUS = "ad_inters_status"
        private const val KEY_AD_REWARD_STATUS = "ad_reward_status"
        private const val KEY_AD_SPLASH_STATUS = "ad_splash_status"
        private const val KEY_AD_FREE_STATUS = "ad_free_status"
        private const val KEY_SPLASH = "splash"
        private const val KEY_BANNER = "banner"
        private const val KEY_BANNER_MINI = "banner_mini"
        private const val KEY_REWARD = "reward"
        private const val KEY_TASK = "task"
        private const val KEY_PURE = "pure"
        private const val KEY_DOWNLOAD = "download"
        private const val KEY_INTERSTITIAL = "interstitial"
        private const val KEY_FREE = "free"
    }
}
