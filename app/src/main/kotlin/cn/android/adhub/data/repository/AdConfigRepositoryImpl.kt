package cn.android.adhub.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.data.remote.RemoteConfigApi
import cn.android.adhub.data.remote.RemoteConfigData
import cn.android.adhub.domain.model.AdChannel
import cn.android.adhub.domain.model.AdPlacement
import cn.android.adhub.domain.repository.AdConfigRepository
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
 */
class AdConfigRepositoryImpl(
    private val adSdkManager: AdSdkManager,
    private val remoteConfigApi: RemoteConfigApi,
) : AdConfigRepository {

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
                adCode = data.optString("adCode", null),
                adStatus = if (data.has("adStatus")) data.getInt("adStatus") else null,
                adSplashCode = data.optString("adSplashCode", null),
                adBannerCode = data.optString("adBannerCode", null),
                adMiniBannerCode = data.optString("adMiniBannerCode", null),
                adRewardCode = data.optString("adRewardCode", null),
                adTaskCode = data.optString("adTaskCode", null),
                adPureCode = data.optString("adPureCode", null),
                adDownloadCode = data.optString("adDownloadCode", null),
                adInterstitialCode = data.optString("adInterstitialCode", null),
                adFreeCode = data.optString("adFreeCode", null),
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
            .putInt(KEY_AD_STATUS, data.adStatus ?: 0)
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
            adStatus = prefs.getInt(KEY_AD_STATUS, 0),
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
        val channel = adSdkManager.currentChannel
        val remote = readCachedConfig()
        // 仅当远程 adType 与当前通道一致时才使用远程代码位（避免 GDT 代码位给 CSJ 用）
        val effectiveRemote = remote?.takeIf { it.adType == channel.code }
        return AdCodeIdResolver.resolve(channel, placement, effectiveRemote)
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
