package cn.manxinghai.zhuimange

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdSdkManager
import cn.manxinghai.zhuimange.data.provider.csj.CsjConfig
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 冷启动开屏 — 完全移植 flutter_merge SplashAdActivity.java 到 Kotlin
 */
class SplashAdActivity : Activity() {

    companion object {
        private const val TAG = "SplashAdActivity"
        private const val SPLASH_TIMEOUT_MS = 5000
        private const val FALLBACK_TIMEOUT_MS = 8000L
        private const val CONFIG_CONNECT_TIMEOUT_MS = 8000
        private const val CONFIG_READ_TIMEOUT_MS = 8000
        private const val REMOTE_AD_CONFIG_URL = "https://adsign.manxinghai.cn/api/app/getAdConfig"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val t0 = System.currentTimeMillis()
    private fun e() = "${System.currentTimeMillis() - t0}ms"

    private lateinit var adContainer: FrameLayout
    private var splashLoadingPanel: View? = null
    private var finished = false
    private var csjCanJump = false
    private var pendingCsjSplash: CSJSplashAd? = null

    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    // ── onCreate: 对齐 flutter_merge 逐行 ──

    override fun onCreate(savedInstanceState: Bundle?) {
        // 替代 Application 类：最早时机初始化
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")
        AppContextHolder.init(applicationContext)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_ad)

        adContainer = findViewById(R.id.splash_ad_container)
        splashLoadingPanel = findViewById(R.id.splash_loading_panel)
        adContainer.visibility = View.GONE
        splashLoadingPanel?.visibility = View.VISIBLE
        applySplashLoadingSystemBars()
        fetchRemoteAdChannelThenBegin()

        Log.d(TAG, "onCreate @${e()}")
    }

    // ── 对齐 applySplashLoadingSystemBars() ──

    private fun applySplashLoadingSystemBars() {
        window?.let { w ->
            w.statusBarColor = Color.TRANSPARENT
            w.navigationBarColor = Color.TRANSPARENT
            w.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    // ── 对齐 fetchRemoteAdChannelThenBegin() ──

    private fun fetchRemoteAdChannelThenBegin() {
        // 远程配置后台拉取（不阻塞）
        ioExecutor.execute { requestAdConfigFromServer() }
        // SDK 初始化立即开始（不等远程配置）
        ensurePrivacyThenLoadAd()
    }

    private fun requestAdConfigFromServer() {
        var connection: HttpURLConnection? = null
        try {
            val requestUrl = "$REMOTE_AD_CONFIG_URL?appPackage=$packageName"
            connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONFIG_CONNECT_TIMEOUT_MS
                readTimeout = CONFIG_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("appName", packageName)
                setRequestProperty("version", readAppVersionName())
            }
            val code = connection.responseCode
            if (code != 200) return
            val body = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: return
            if (body.isEmpty()) return
            val json = JSONObject(body.trim())
            val apiCode = json.optInt("code", 0)
            if (apiCode != 0 && apiCode != 200) return
            val data = json.optJSONObject("data") ?: return
            // 缓存到 SharedPreferences
            cacheAdConfig(data)
        } catch (_: Exception) {
        } finally {
            connection?.disconnect()
        }
    }

    private fun cacheAdConfig(data: JSONObject) {
        getSharedPreferences("ad_remote_config", MODE_PRIVATE).edit()
            .putString("splash", data.optString("adSplashCode", ""))
            .apply()
    }

    @Suppress("DEPRECATION")
    private fun readAppVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }

    // ── 对齐 ensurePrivacyThenLoadAdCsj() ──

    private fun ensurePrivacyThenLoadAd() {
        if (!CsjAdSdkManager.isPrivacyAgreed(this)) {
            CsjAdSdkManager.setPrivacyAgreed(this, true)
        }
        startLoadWhenReadyCsj()
    }

    // ── 对齐 startLoadWhenReadyCsj() ──

    private fun startLoadWhenReadyCsj() {
        CsjAdSdkManager.ensureReady(this, object : CsjAdSdkManager.ReadyCallback {
            override fun onReady() {
                Log.d(TAG, "SDK 就绪 @${e()}")
                loadSplashAdCsj()
            }
            override fun onFailed() {
                Log.w(TAG, "SDK 失败 @${e()}")
                goMainCsj()
            }
        })
    }

    // ── 对齐 loadSplashAdCsj() ──

    private fun loadSplashAdCsj() {
        mainHandler.postDelayed({ goMainCsj() }, FALLBACK_TIMEOUT_MS)

        val adNative: TTAdNative = TTAdSdk.getAdManager().createAdNative(this)
        val dm: DisplayMetrics = resources.displayMetrics

        val prefs = getSharedPreferences("ad_remote_config", MODE_PRIVATE)
        val codeId = prefs.getString("splash", null) ?: CsjConfig.SPLASH_CODE_ID

        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setExpressViewAcceptedSize(dm.widthPixels / dm.density, dm.heightPixels / dm.density)
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setMediationSplashRequestInfo(csjSplashRequestInfo)
                    .build()
            )
            .build()

        adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd) {}
            override fun onSplashLoadFail(error: CSJAdError) { goMainCsj() }
            override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                if (ad == null || adContainer == null) { goMainCsj(); return }
                pendingCsjSplash = ad
                onSplashAdPreloadReady()
            }
            override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) { goMainCsj() }
        }, SPLASH_TIMEOUT_MS)
    }

    // ── 对齐 onSplashAdPreloadReady() + tryShowPreloadedSplashAd() ──

    private fun onSplashAdPreloadReady() {
        if (finished || isFinishing) return
        splashLoadingPanel?.visibility = View.GONE
        showPreloadedCsjSplash()
    }

    // ── 对齐 showPreloadedCsjSplash() ──

    private fun showPreloadedCsjSplash() {
        val ad = pendingCsjSplash
        pendingCsjSplash = null
        if (ad == null || adContainer == null) { goMainCsj(); return }
        mainHandler.removeCallbacksAndMessages(null)
        adContainer.visibility = View.VISIBLE
        Log.i(TAG, "展示开屏广告 @${e()}")
        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(ad: CSJSplashAd) { Log.i(TAG, "广告已展示 @${e()}") }
            override fun onSplashAdClick(ad: CSJSplashAd) {}
            override fun onSplashAdClose(ad: CSJSplashAd, closeType: Int) {
                Log.i(TAG, "广告关闭 @${e()}")
                mainHandler.post { nextCsj() }
            }
        })
        ad.showSplashView(adContainer)
    }

    // ── 对齐 nextCsj() / goMainCsj() ──

    private fun nextCsj() = if (csjCanJump) goMainCsj() else run { csjCanJump = true }
    private fun goMainCsj() {
        if (finished || isFinishing) return
        finished = true
        Log.i(TAG, "进入主页 @${e()}")
        mainHandler.removeCallbacksAndMessages(null)
        startActivity(Intent(this, MainActivity::class.java).putExtra("cold_start", true))
        finish()
    }

    override fun onResume() { super.onResume(); if (csjCanJump) goMainCsj(); csjCanJump = true }
    override fun onPause() { super.onPause(); csjCanJump = false }
    override fun onDestroy() { mainHandler.removeCallbacksAndMessages(null); ioExecutor.shutdown(); super.onDestroy() }
}
