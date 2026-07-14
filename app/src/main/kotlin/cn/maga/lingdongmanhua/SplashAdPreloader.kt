package cn.maga.lingdongmanhua

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import cn.maga.lingdongmanhua.data.provider.csj.CsjAdSdkManager
import cn.maga.lingdongmanhua.data.provider.csj.CsjConfig
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 开屏广告加载辅助类。
 *
 * 当前用途：
 * - 在 SplashAdActivity 内按需启动广告加载（不再在 Application 中预加载）
 * - 异步拉取远程广告配置并缓存到 SharedPreferences
 *
 * 旧的 Application 预加载入口 [start] 仍保留，但已不再使用。
 */
object SplashAdPreloader {
    private const val TAG = "SplashAdPreloader"
    private const val PREFS_NAME = "ad_remote_config"
    private const val SPLASH_TIMEOUT_MS = 5000
    private const val CONFIG_CONNECT_TIMEOUT_MS = 5000
    private const val CONFIG_READ_TIMEOUT_MS = 5000
    private const val REMOTE_AD_CONFIG_URL = "https://adsign.manxinghai.cn/api/app/getAdConfig"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** 预加载的开屏广告（渲染成功，待展示） */
    @Volatile
    var pendingCsjSplash: CSJSplashAd? = null
        private set

    /** 预加载是否已完成（成功或失败） */
    @Volatile
    var preloadFinished = false
        private set

    /** 预加载是否成功 */
    @Volatile
    var preloadSuccess = false
        private set

    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    /**
     * 在 Application.onCreate 中预加载的入口。
     * 当前未使用，保留以便后续需要时重新启用。
     */
    fun start(context: Context) {
        Log.i(TAG, "预加载启动")
        pendingCsjSplash = null
        preloadFinished = false
        preloadSuccess = false

        if (!CsjAdSdkManager.isPrivacyAgreed(context)) {
            CsjAdSdkManager.setPrivacyAgreed(context, true)
        }

        fetchRemoteConfigAsync(context)

        CsjAdSdkManager.ensureReady(context, object : CsjAdSdkManager.ReadyCallback {
            override fun onReady() {
                Log.i(TAG, "SDK 就绪，开始预加载开屏广告")
                preloadSplashAd(context)
            }

            override fun onFailed() {
                Log.w(TAG, "SDK 初始化失败，预加载终止")
                preloadFinished = true
                preloadSuccess = false
            }
        })
    }

    private fun preloadSplashAd(context: Context) {
        val codeId = getSplashCodeId(context)
        Log.i(TAG, "预加载开屏广告: codeId=$codeId")

        val dm = context.resources.displayMetrics
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setExpressViewAcceptedSize(
                dm.widthPixels / dm.density,
                dm.heightPixels / dm.density
            )
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setMediationSplashRequestInfo(csjSplashRequestInfo)
                    .build()
            )
            .build()

        val adNative = TTAdSdk.getAdManager().createAdNative(context)

        adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                Log.i(TAG, "预加载: 广告加载成功")
            }

            override fun onSplashLoadFail(error: CSJAdError) {
                Log.e(TAG, "预加载: 广告加载失败 code=${error.code} msg=${error.msg}")
                preloadFinished = true
                preloadSuccess = false
            }

            override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                Log.i(TAG, "预加载: 广告渲染成功")
                pendingCsjSplash = ad
                preloadFinished = true
                preloadSuccess = true
            }

            override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                Log.e(TAG, "预加载: 广告渲染失败 code=${error.code} msg=${error.msg}")
                preloadFinished = true
                preloadSuccess = false
            }
        }, SPLASH_TIMEOUT_MS)
    }

    /**
     * 消费预加载的广告（取走后不可再用）。
     * @return 预加载的 CSJSplashAd，或 null（未就绪/已消费/失败）
     */
    fun consumePendingAd(): CSJSplashAd? {
        val ad = pendingCsjSplash
        pendingCsjSplash = null
        return ad
    }

    // ── 远程配置 ──

    /** 异步拉取（不阻塞调用线程） */
    fun fetchRemoteConfigAsync(context: Context) {
        ioExecutor.execute { fetchRemoteConfigSync(context) }
    }

    /** 同步拉取（调用线程执行 HTTP + 缓存，供外部 IO 线程使用） */
    fun fetchRemoteConfigSync(context: Context) {
        val remoteConfig = requestAdConfigFromServer(context)
        if (remoteConfig != null) {
            cacheAdConfig(context, remoteConfig)
            Log.i(TAG, "远程配置已缓存")
        } else {
            Log.w(TAG, "远程配置拉取失败，使用本地缓存")
        }
    }

    private fun requestAdConfigFromServer(context: Context): JSONObject? {
        var connection: HttpURLConnection? = null
        try {
            val requestUrl = "$REMOTE_AD_CONFIG_URL?appPackage=${context.packageName}"
            connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONFIG_CONNECT_TIMEOUT_MS
                readTimeout = CONFIG_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("appName", context.packageName)
                setRequestProperty("version", readAppVersionName(context))
            }
            val code = connection.responseCode
            if (code != 200) {
                Log.w(TAG, "requestAdConfigFromServer failed, code=$code")
                return null
            }
            val body = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: return null
            if (body.isEmpty()) return null

            val json = JSONObject(body.trim())
            val apiCode = json.optInt("code", 0)
            if (apiCode != 0 && apiCode != 200) {
                Log.w(TAG, "globalSetting code=$apiCode")
                return null
            }
            return json.optJSONObject("data")
        } catch (e: Exception) {
            Log.w(TAG, "requestAdConfigFromServer failed", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun cacheAdConfig(context: Context, data: JSONObject) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("ad_type", if (data.has("adType")) data.getInt("adType") else -1)
            .putString("splash", data.optString("adSplashCode", ""))
            .putString("banner", data.optString("adBannerCode", ""))
            .putString("banner_mini", data.optString("adMiniBannerCode", ""))
            .putString("reward", data.optString("adRewardCode", ""))
            .putString("task", data.optString("adTaskCode", ""))
            .putString("pure", data.optString("adPureCode", ""))
            .putString("download", data.optString("adDownloadCode", ""))
            .putString("interstitial", data.optString("adInterstitialCode", ""))
            .putString("free", data.optString("adFreeCode", ""))
            .putInt("ad_status", if (data.has("adStatus")) data.getInt("adStatus") else 0)
            .apply()
    }

    private fun getSplashCodeId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remote = prefs.getString("splash", null)
        return if (!remote.isNullOrEmpty()) remote else CsjConfig.SPLASH_CODE_ID
    }

    @Suppress("DEPRECATION")
    private fun readAppVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
    }
}
