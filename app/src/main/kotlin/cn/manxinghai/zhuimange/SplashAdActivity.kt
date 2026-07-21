package cn.manxinghai.zhuimange

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cn.manxinghai.zhuimange.core.HotStartInterstitialManager
import cn.manxinghai.zhuimange.core.PureModeManager
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdSdkManager
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import cn.manxinghai.zhuimange.ui.AdHubApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject

/**
 * 冷启动开屏 Activity（LAUNCHER）。
 *
 * 两段超时：
 * - 阶段 1（启动超时 8s）：SDK 初始化 + 广告加载
 * - 阶段 2（广告超时 10s）：广告展示中兜底
 *
 * 对齐 flutter_merge：CSJ SDK 先设隐私同意再初始化。
 */
class SplashAdActivity : ComponentActivity() {

    private val adSdkManager: AdSdkManager by inject()
    private val adConfigRepo: AdConfigRepository by inject()
    private val pureModeManager: PureModeManager by inject()
    private val hotStartManager: HotStartInterstitialManager by inject()

    private var adContainer: ViewGroup? = null
    private var homeComposeView: ComposeView? = null
    private var finished = false

    @Volatile private var keepSplash = true
    private var launchTimeoutJob: Job? = null
    private var adTimeoutJob: Job? = null

    companion object {
        private const val TAG = "SplashAdActivity"
        private const val LAUNCH_TIMEOUT_MS = 8000L
        private const val AD_TIMEOUT_MS = 10_000L
    }

    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常: ${throwable.message}", throwable)
        keepSplash = false
        cancelAllTimeouts()
        goToHome()
    }

    private val t0 = System.currentTimeMillis()
    private fun e() = "${System.currentTimeMillis() - t0}ms"

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── 0. 隐私同意（CSJ SDK 必需，早于任何 SDK 操作） ──
        CsjAdSdkManager.setPrivacyAgreed(this, true)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate @${e()} — 系统 splash 就位")
        setContentView(R.layout.activity_splash_ad)
        adContainer = findViewById(R.id.splash_ad_container)
        homeComposeView = findViewById(R.id.home_compose_view)

        homeComposeView?.setContent { AdHubApp() }
        hotStartManager.bindActivity(this)

        // 阶段 1：启动超时
        launchTimeoutJob = (lifecycleScope + ceh).launch {
            delay(LAUNCH_TIMEOUT_MS)
            Log.w(TAG, "启动超时 @${e()} — 降级进入主页")
            keepSplash = false
            goToHome()
        }

        // 冷启动流程
        (lifecycleScope + ceh).launch {
            try { startColdFlow() }
            catch (ex: Exception) {
                Log.e(TAG, "startColdFlow 异常: ${ex.message}", ex)
                keepSplash = false
                cancelAllTimeouts()
                goToHome()
            }
        }
    }

    private suspend fun startColdFlow() {
        Log.d(TAG, "startColdFlow 开始 @${e()}")

        if (pureModeManager.checkActive()) {
            Log.i(TAG, "纯净模式已激活，跳过开屏广告")
            keepSplash = false
            cancelAllTimeouts()
            goToHome()
            return
        }

        // 等待 SDK 就绪（CsjAdSdkManager 负责 TTAdSdk.init + start）
        val provider = adSdkManager.currentProvider
        try {
            val initResult = provider.initialize(this)
            if (initResult.isFailure) {
                Log.w(TAG, "SDK 初始化失败: ${initResult.exceptionOrNull()?.message}")
                keepSplash = false
                cancelAllTimeouts()
                goToHome()
                return
            }
            Log.d(TAG, "SDK 就绪 @${e()}")
        } catch (ex: Exception) {
            Log.e(TAG, "SDK 初始化异常: ${ex.message}", ex)
            keepSplash = false
            cancelAllTimeouts()
            goToHome()
            return
        }

        // 拉远程广告配置（优先用服务端下发的代码位和通道）
        try {
            val remote = adConfigRepo.fetchAdConfig()
            if (remote != null) {
                Log.i(TAG, "远程广告配置已加载: adType=${remote.adType} splashCode=${remote.adSplashCode}")
                remote.adType?.let {
                    adSdkManager.applyRemoteAdType(it)
                    adSdkManager.ensureProviderReady()
                    Log.i(TAG, "通道已切换: ${adSdkManager.currentChannel.displayName}")
                }
            }
        } catch (ex: Exception) {
            Log.w(TAG, "拉取远程广告配置失败，使用本地缓存/硬编码: ${ex.message}")
        }

        val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
        Log.d(TAG, "开屏代码位: $codeId")
        if (codeId.isEmpty()) {
            Log.w(TAG, "开屏代码位为空，跳过")
            keepSplash = false
            cancelAllTimeouts()
            goToHome()
            return
        }

        val container = adContainer
        if (container == null || isFinishing || isDestroyed) {
            keepSplash = false
            cancelAllTimeouts()
            goToHome()
            return
        }

        Log.i(TAG, "开始展示开屏广告 channel=${adSdkManager.currentChannel.displayName} @${e()}")

        try {
            val shown = provider.showSplashAd(
                activity = this,
                codeId = codeId,
                container = container,
                onAdLoaded = {
                    Log.i(TAG, "广告素材就绪 @${e()} — 取消启动超时")
                    launchTimeoutJob?.cancel()
                    launchTimeoutJob = null
                },
                onAdShown = {
                    Log.i(TAG, "广告已展示 @${e()} — 释放系统 splash")
                    keepSplash = false
                    adTimeoutJob = (lifecycleScope + ceh).launch {
                        delay(AD_TIMEOUT_MS)
                        if (!finished) {
                            Log.w(TAG, "广告超时 @${e()} — 强制降级")
                            goToHome()
                        }
                    }
                },
            )
            Log.i(TAG, if (shown) "开屏广告已关闭 @${e()}" else "开屏广告未展示（无填充或渲染失败）@${e()}")
        } catch (ex: Exception) {
            Log.e(TAG, "开屏广告异常: ${ex.message}", ex)
        }

        cancelAllTimeouts()
        goToHome()
    }

    private fun cancelAllTimeouts() {
        launchTimeoutJob?.cancel(); launchTimeoutJob = null
        adTimeoutJob?.cancel(); adTimeoutJob = null
    }

    private fun goToHome() {
        if (finished || isFinishing || isDestroyed) return
        finished = true
        keepSplash = false
        adContainer?.visibility = View.GONE
        Log.i(TAG, "进入主页 @${e()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllTimeouts()
        hotStartManager.unbindActivity(this)
        adContainer = null
        homeComposeView = null
    }
}
