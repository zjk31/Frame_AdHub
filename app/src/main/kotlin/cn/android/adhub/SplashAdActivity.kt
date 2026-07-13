package cn.android.adhub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cn.android.adhub.core.HotStartInterstitialManager
import cn.android.adhub.core.PureModeManager
import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.domain.model.AdPlacement
import cn.android.adhub.domain.repository.AdConfigRepository
import cn.android.adhub.ui.AdHubApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * 冷启动开屏 Activity（LAUNCHER）—— 单 Activity，首帧即渲染 Compose。
 *
 * 两段超时：
 * - 阶段 1（启动超时 8s）：SDK 初始化 + 广告加载，超时未 show → 降级进首页
 * - 阶段 2（广告超时 10s）：广告展示中，超时未 close → 强制降级进首页
 */
class SplashAdActivity : ComponentActivity() {

    private val adSdkManager: AdSdkManager by inject()
    private val adConfigRepo: AdConfigRepository by inject()
    private val pureModeManager: PureModeManager by inject()
    private val hotStartManager: HotStartInterstitialManager by inject()

    private var adContainer: ViewGroup? = null
    private var homeComposeView: ComposeView? = null
    private var finished = false

    @Volatile
    private var keepSplash = true

    /** 阶段 1 启动超时 Job，广告开始展示后取消 */
    private var launchTimeoutJob: Job? = null
    /** 阶段 2 广告超时 Job，广告关闭后取消 */
    private var adTimeoutJob: Job? = null

    companion object {
        private const val TAG = "SplashAdActivity"
        /** 阶段 1：SDK 初始化 + 广告加载的最大等待时间（穿山甲 SDK 内部 SdkSettings 远程配置拉取可能需要 10-12s） */
        private const val LAUNCH_TIMEOUT_MS = 15_000L
        /** 阶段 2：广告展示的最大等待时间（兜底，正常由 SDK 自己关闭） */
        private const val AD_TIMEOUT_MS = 10_000L
    }

    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常: ${throwable.message}", throwable)
        keepSplash = false
        cancelAllTimeouts()
        goToHome()
    }

    private val onCreateTime = System.currentTimeMillis()

    private fun elapsed() = "${System.currentTimeMillis() - onCreateTime}ms"

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate @${elapsed()} — 系统 splash 已就位")
        setContentView(R.layout.activity_splash_ad)
        adContainer = findViewById(R.id.splash_ad_container)
        homeComposeView = findViewById(R.id.home_compose_view)

        homeComposeView?.setContent { AdHubApp() }
        hotStartManager.bindActivity(this)
        Log.d(TAG, "Compose 首页预渲染已启动")

        // 阶段 1：启动超时 —— 跑在 Default 线程，不受主线程阻塞影响
        launchTimeoutJob = (lifecycleScope + ceh).launch(Dispatchers.Default) {
            delay(LAUNCH_TIMEOUT_MS)
            Log.w(TAG, "启动超时 @${elapsed()} — 降级进入主页")
            keepSplash = false
            withContext(Dispatchers.Main) { goToHome() }
        }

        // 冷启动流程（在 Default 线程执行，避免主线程被 SDK 内部任务阻塞导致 delay() 恢复延迟）
        (lifecycleScope + ceh).launch(Dispatchers.Default) {
            try {
                startColdFlow()
            } catch (e: Exception) {
                Log.e(TAG, "startColdFlow 异常: ${e.message}", e)
                keepSplash = false
                cancelAllTimeouts()
                withContext(Dispatchers.Main) { goToHome() }
            }
        }
    }

    private suspend fun startColdFlow() {
        Log.d(TAG, "startColdFlow 开始")

        if (pureModeManager.checkActive()) {
            Log.i(TAG, "纯净模式已激活，跳过开屏广告")
            keepSplash = false
            cancelAllTimeouts()
            withContext(Dispatchers.Main) { goToHome() }
            return
        }

        // 等待 SDK 就绪
        Log.d(TAG, "等待 SDK 就绪…")
        try {
            val provider = adSdkManager.currentProvider
            // 轮询等待，50ms 间隔，最多 3s
            var waited = 0
            while (waited < 3000) {
                if (provider.isReady()) break
                delay(50)
                waited += 50
            }
            if (!provider.isReady()) {
                Log.w(TAG, "SDK 仍未就绪，继续尝试展示广告")
            }
        } catch (e: Exception) {
            Log.e(TAG, "等待 SDK 就绪异常: ${e.message}", e)
            keepSplash = false
            cancelAllTimeouts()
            withContext(Dispatchers.Main) { goToHome() }
            return
        }
        Log.d(TAG, "SDK 就绪 @${elapsed()}")

        // 对齐 flutter_merge：SDK 就绪后拉远程广告配置，优先用服务端下发的代码位和通道
        try {
            Log.d(TAG, "开始拉取远程广告配置…")
            val remote = adConfigRepo.fetchAdConfig()
            if (remote != null) {
                Log.i(TAG, "远程广告配置已加载 @${elapsed()}: adType=${remote.adType} splashCode=${remote.adSplashCode}")
                // 服务端下发的 adType 可能跟本地缓存不一致，切换通道并初始化
                remote.adType?.let {
                    adSdkManager.applyRemoteAdType(it)
                    adSdkManager.ensureProviderReady()
                    Log.i(TAG, "通道已切换: ${adSdkManager.currentChannel.displayName}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "拉取远程广告配置失败，使用本地缓存/硬编码: ${e.message}")
        }

        val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
        Log.d(TAG, "开屏代码位: $codeId")
        if (codeId.isEmpty()) {
            Log.w(TAG, "开屏代码位为空，跳过")
            keepSplash = false
            cancelAllTimeouts()
            withContext(Dispatchers.Main) { goToHome() }
            return
        }

        val provider = adSdkManager.currentProvider
        val container = adContainer
        if (container == null || isFinishing || isDestroyed) {
            keepSplash = false
            cancelAllTimeouts()
            withContext(Dispatchers.Main) { goToHome() }
            return
        }

        Log.d(TAG, "当前通道: ${provider.javaClass.simpleName}")
        Log.i(TAG, "开始预加载开屏广告…")

        try {
            // showSplashAd 内部 UI 操作已通过 container.post 切回主线程，
            // 此处无需 withContext(Dispatchers.Main)，避免主线程被 SDK 初始化阻塞时协程饿死
            val shown = provider.showSplashAd(
                activity = this@SplashAdActivity,
                codeId = codeId,
                container = container,
                onAdLoaded = {
                    // 广告素材就绪 → 取消启动超时
                    Log.i(TAG, "广告素材就绪 @${elapsed()} — 取消启动超时")
                    launchTimeoutJob?.cancel()
                    launchTimeoutJob = null
                },
                onAdShown = {
                    // 广告真正可见 → 释放系统 splash → 启动广告超时
                    Log.i(TAG, "广告已展示 @${elapsed()} — 释放系统 splash，启动广告超时")
                    keepSplash = false
                    adTimeoutJob = (lifecycleScope + ceh).launch {
                        delay(AD_TIMEOUT_MS)
                        if (!finished) {
                            Log.w(TAG, "广告超时 @${elapsed()} — 强制降级")
                            goToHome()
                        }
                    }
                },
            )
            Log.i(TAG, if (shown) "开屏广告已关闭 @${elapsed()}" else "开屏广告未展示（无填充或渲染失败）@${elapsed()}")
        } catch (e: Exception) {
            Log.e(TAG, "开屏广告异常: ${e.message}", e)
        }

        // 广告关闭（正常或异常） → 取消广告超时 → 进首页
        cancelAllTimeouts()
        withContext(Dispatchers.Main) { goToHome() }
    }

    private fun cancelAllTimeouts() {
        launchTimeoutJob?.cancel()
        launchTimeoutJob = null
        adTimeoutJob?.cancel()
        adTimeoutJob = null
    }

    private fun goToHome() {
        if (finished || isFinishing || isDestroyed) return
        finished = true
        keepSplash = false
        adContainer?.visibility = View.GONE
        Log.i(TAG, "进入主页 @${elapsed()} — adContainer GONE, Compose 露出")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllTimeouts()
        hotStartManager.unbindActivity(this)
        adContainer = null
        homeComposeView = null
    }
}
