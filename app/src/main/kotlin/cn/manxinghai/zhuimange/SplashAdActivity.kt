package cn.manxinghai.zhuimange

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.data.local.TokenStore
import cn.manxinghai.zhuimange.data.provider.baidu.BaiduAdProvider
import cn.manxinghai.zhuimange.data.provider.baidu.BaiduConfig
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdProvider
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdSdkManager
import cn.manxinghai.zhuimange.data.provider.csj.CsjConfig
import cn.manxinghai.zhuimange.data.provider.gdt.GdtAdProvider
import cn.manxinghai.zhuimange.data.provider.gdt.GdtConfig
import cn.manxinghai.zhuimange.data.provider.umeng.UmengAdProvider
import cn.manxinghai.zhuimange.data.provider.umeng.UmengConfig
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.provider.AdProvider
import cn.manxinghai.zhuimange.ui.components.PrivacyConsentHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 冷启动开屏 — 多通道架构。
 *
 * 通道选择：读 SharedPreferences 缓存的 adType（子 App 自行写入），
 * 若无缓存默认 CSJ，代码位同理优先缓存其次硬编码。
 */
class SplashAdActivity : Activity() {

    companion object {
        private const val TAG = "SplashAdActivity"
        private const val FALLBACK_TIMEOUT_MS = 8000L
        private const val PREFS_NAME = "ad_remote_config"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val splashScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val t0 = System.currentTimeMillis()
    private fun e() = "${System.currentTimeMillis() - t0}ms"

    private lateinit var adContainer: FrameLayout
    private var splashLoadingPanel: View? = null
    private var finished = false
    private var canJump = false

    // ── onCreate ──

    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")
        AppContextHolder.init(applicationContext)

        super.onCreate(savedInstanceState)

        // 隐私同意（CSJ 等 SDK 必需，早于任何 SDK 操作）
        if (PrivacyConsentHelper.showIfNeeded(this)) return

        setContentView(R.layout.activity_splash_ad)

        adContainer = findViewById(R.id.splash_ad_container)
        splashLoadingPanel = findViewById(R.id.splash_loading_panel)
        val versionText = findViewById<TextView>(R.id.splash_version)
        versionText.text = "v${readAppVersionName()}"
        adContainer.visibility = View.GONE
        splashLoadingPanel?.visibility = View.VISIBLE
        applySplashLoadingSystemBars()

        loadSplashAd()
        Log.d(TAG, "onCreate @${e()}")
    }

    private fun applySplashLoadingSystemBars() {
        @Suppress("DEPRECATION")
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

    // ── 多通道开屏加载 ──

    private fun loadSplashAd() {
        // 读缓存（子 App 负责写入），默认 CSJ
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val adType = prefs.getInt("ad_type", AdChannel.Csj.code)
        val channel = AdChannel.fromCode(adType)

        // 代码位：优先缓存，其次通道硬编码
        val cachedCode = prefs.getString("splash", null)
        val codeId = if (!cachedCode.isNullOrEmpty()) cachedCode else when (channel) {
            AdChannel.Umeng -> UmengConfig.CODE_ID_SPLASH
            AdChannel.Csj   -> CsjConfig.SPLASH_CODE_ID
            AdChannel.Gdt   -> GdtConfig.SPLASH_POS_ID
            AdChannel.Baidu -> BaiduConfig.SPLASH_PLACE_ID
        }

        val provider: AdProvider = when (channel) {
            AdChannel.Umeng -> UmengAdProvider()
            AdChannel.Csj   -> CsjAdProvider(TokenStore(applicationContext))
            AdChannel.Gdt   -> GdtAdProvider()
            AdChannel.Baidu -> BaiduAdProvider(TokenStore(applicationContext))
        }

        // 所有广告 SDK 都需要隐私同意
        CsjAdSdkManager.setPrivacyAgreed(this, true)

        Log.i(TAG, "loadSplashAd adType=$adType channel=${channel.displayName} codeId=$codeId @${e()}")

        // 8s 超时兜底
        mainHandler.postDelayed({ goMain() }, FALLBACK_TIMEOUT_MS)

        splashScope.launch {
            val initResult = provider.initialize(this@SplashAdActivity)
            if (initResult.isFailure) {
                Log.w(TAG, "Provider init failed: ${initResult.exceptionOrNull()?.message}")
                goMain()
                return@launch
            }
            provider.showSplashAd(
                activity = this@SplashAdActivity,
                codeId = codeId,
                container = adContainer,
                onAdLoaded = {
                    mainHandler.removeCallbacksAndMessages(null)
                    splashLoadingPanel?.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                },
                onAdShown = {
                    // 广告已可见
                },
            )
            goMain()
        }
    }

    @Suppress("DEPRECATION")
    private fun readAppVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }

    // ── 跳转主页 ──

    private fun goMain() {
        if (finished || isFinishing) return
        finished = true
        Log.i(TAG, "进入主页 @${e()}")
        mainHandler.removeCallbacksAndMessages(null)
        startActivity(Intent(this, MainActivity::class.java).putExtra("cold_start", true))
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onResume() { super.onResume(); if (canJump) goMain(); canJump = true }
    override fun onPause() { super.onPause(); canJump = false }
    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        splashScope.cancel()
        super.onDestroy()
    }
}
