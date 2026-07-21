package cn.manxinghai.zhuimange.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.data.remote.RemoteConfigApi
import cn.manxinghai.zhuimange.data.remote.RemoteConfigData
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * [AdConfigRepository] 实现。
 *
 * 对齐 flutter_merge：
 * - [fetchAdConfig] 从 adsign.manxinghai.cn 拉取完整广告配置，缓存到 SharedPreferences
 * - [getCodeId] 优先使用远程下发的代码位，不可用时回退硬编码
 * - 支持服务端关闭单类广告（per-ad-type 开关）
 * - SSE 推送 updateAdConfig → 调用 [fetchAdConfig] 实时切换广告通道
 */
class AdConfigRepositoryImpl(
    private val adSdkManager: AdSdkManager,
    private val remoteConfigApi: RemoteConfigApi,
) : AdConfigRepository {

    private val _configVersion = MutableStateFlow(0)
    /** 配置版本号，每次 fetchAdConfig 更新后递增，UI 层可 collect 此 Flow 响应变化 */
    override val configVersion: StateFlow<Int> = _configVersion.asStateFlow()

    private val prefs: SharedPreferences
        get() = AppContextHolder.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun fetchRemoteChannel(): Result<AdChannel> {
        return adSdkManager.refreshRemoteChannel().map { adSdkManager.currentChannel }
    }

    override suspend fun fetchAdConfig(): RemoteConfigData? {
        return try {
            val data = withContext(Dispatchers.IO) { requestAdConfigFromServer() }
            if (data != null) {
                cacheConfig(data)
                // SSE 推送 updateAdConfig → 立即切换当前广告通道
                data.adType?.let { adSdkManager.applyRemoteAdType(it) }
                _configVersion.value++  // 通知 UI 刷新
                Log.i(TAG, "fetchAdConfig 成功: adType=${data.adType} splashCode=${data.adSplashCode}")
            }
            data ?: readCachedConfig()
        } catch (e: Exception) {
            Log.w(TAG, "fetchAdConfig 异常: ${e.message}，读取缓存")
            readCachedConfig()
        }
    }

    // ── 对齐 flutter_merge: 直接 HTTP GET 请求广告配置接口 ──

    private fun requestAdConfigFromServer(): RemoteConfigData? {
        var connection: HttpURLConnection? = null
        try {
            val pkg = AppContextHolder.context.packageName
            val requestUrl = "$REMOTE_AD_CONFIG_URL?appPackage=$pkg"
            connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONFIG_TIMEOUT_MS
                readTimeout = CONFIG_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("appName", pkg)
                setRequestProperty("version", readAppVersion())
            }
            val code = connection.responseCode
            if (code != 200) {
                Log.w(TAG, "requestAdConfigFromServer 失败: code=$code")
                return null
            }
            val body = connection.inputStream?.bufferedReader()?.readText() ?: return null
            if (body.isEmpty()) return null

            val json = JSONObject(body.trim())
            val apiCode = json.optInt("code", 0)
            if (apiCode != 0 && apiCode != 200) {
                Log.w(TAG, "requestAdConfigFromServer code=$apiCode")
                return null
            }
            val data = json.optJSONObject("data") ?: return null
            return RemoteConfigData(
                adType = if (data.has("adType")) data.getInt("adType") else null,
                adCode = data.optString("adCode", "").ifEmpty { null },
                adStatus = if (data.has("adStatus")) data.getInt("adStatus") else null,
                adBannerStatus = if (data.has("adBannerStatus")) data.getInt("adBannerStatus") else null,
                adIntersStatus = if (data.has("adIntersStatus")) data.getInt("adIntersStatus") else null,
                adRewardStatus = if (data.has("adRewardStatus")) data.getInt("adRewardStatus") else null,
                adSplashStatus = if (data.has("adSplashStatus")) data.getInt("adSplashStatus") else null,
                adFreeStatus = if (data.has("adFreeStatus")) data.getInt("adFreeStatus") else null,
                adSplashCode = data.optString("adSplashCode", "").ifEmpty { null },
                adBannerCode = data.optString("adBannerCode", "").ifEmpty { null },
                adMiniBannerCode = data.optString("adMiniBannerCode", "").ifEmpty { null },
                adRewardCode = data.optString("adRewardCode", "").ifEmpty { null },
                adTaskCode = data.optString("adTaskCode", "").ifEmpty { null },
                adPureCode = data.optString("adPureCode", "").ifEmpty { null },
                adDownloadCode = data.optString("adDownloadCode", "").ifEmpty { null },
                adInterstitialCode = data.optString("adInterstitialCode", "").ifEmpty { null },
                adFreeCode = data.optString("adFreeCode", "").ifEmpty { null },
            )
        } catch (e: Exception) {
            Log.w(TAG, "requestAdConfigFromServer 异常", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun readAppVersion(): String {
        return try {
            val pkg = AppContextHolder.context.packageName
            val info = AppContextHolder.context.packageManager.getPackageInfo(pkg, 0)
            info.versionName ?: ""
        } catch (e: Exception) { "" }
    }

    // ── 本地缓存 ──

    private fun cacheConfig(data: RemoteConfigData) {
        prefs.edit()
            .putInt(KEY_AD_TYPE, data.adType ?: -1)
            .putString(KEY_AD_CODE, data.adCode)
            .putInt(KEY_AD_STATUS, data.adStatus ?: 1)
            .putInt(KEY_AD_BANNER_STATUS, data.adBannerStatus ?: 1)
            .putInt(KEY_AD_INTERS_STATUS, data.adIntersStatus ?: 1)
            .putInt(KEY_AD_REWARD_STATUS, data.adRewardStatus ?: 1)
            .putInt(KEY_AD_SPLASH_STATUS, data.adSplashStatus ?: 1)
            .putInt(KEY_AD_FREE_STATUS, data.adFreeStatus ?: 1)
            .putString(KEY_SPLASH, data.adSplashCode)
            .putString(KEY_BANNER, data.adBannerCode)
            .putString(KEY_BANNER_MINI, data.adMiniBannerCode)
            .putString(KEY_REWARD, data.adRewardCode)
            .putString(KEY_TASK, data.adTaskCode)
            .putString(KEY_PURE, data.adPureCode)
            .putString(KEY_DOWNLOAD, data.adDownloadCode)
            .putString(KEY_INTERSTITIAL, data.adInterstitialCode)
            .putString(KEY_FREE, data.adFreeCode)
            .apply()
    }

    private fun readCachedConfig(): RemoteConfigData? {
        if (!prefs.contains(KEY_SPLASH)) return null
        return RemoteConfigData(
            adType = prefs.getInt(KEY_AD_TYPE, -1).takeIf { it >= 0 },
            adCode = prefs.getString(KEY_AD_CODE, null),
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
        // 总开关关闭
        if (remote?.adStatus == 0) return ""
        // 分类开关关闭则返回空（服务端可单独关某类广告）
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
        private const val TAG = "AdConfigRepo"
        /** 对齐 flutter_merge MergeRemoteConfig.REMOTE_AD_CHANNEL_URL */
        private const val REMOTE_AD_CONFIG_URL = "https://adsign.manxinghai.cn/api/app/getAdConfig"
        private const val CONFIG_TIMEOUT_MS = 8000
        private const val PREFS_NAME = "ad_remote_config"
        private const val KEY_AD_TYPE = "ad_type"
        private const val KEY_AD_CODE = "ad_code"
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
