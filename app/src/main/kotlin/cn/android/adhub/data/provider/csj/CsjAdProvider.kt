package cn.android.adhub.data.provider.csj

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.domain.model.AdLoadState
import cn.android.adhub.domain.model.RewardResult
import cn.android.adhub.domain.provider.AdProvider
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CsjAdProvider(
    private val tokenRepo: cn.android.adhub.domain.repository.TokenRepository,
) : AdProvider {

    // ── 状态标志（对齐 flutter_merge CsjAdSdkManager） ──

    @Volatile private var initialized = false
    @Volatile private var starting = false
    @Volatile private var started = false

    // ── 公开初始化入口 ──

    override suspend fun initialize(context: Context): Result<Unit> {
        initIfNeeded(context)
        return ensureReady()
    }

    // ── 第一步：TTAdSdk.init（同步，只调一次） ──

    private fun initIfNeeded(context: Context) {
        if (initialized) return
        val config = CsjConfig.buildAdConfig(context)
        val initResult = TTAdSdk.init(context.applicationContext, config)
        android.util.Log.i("CsjAdProvider", "TTAdSdk.init result=$initResult appId=${CsjConfig.APP_ID} isSdkReady=${TTAdSdk.isSdkReady()}")
        // 若 SDK 已被 ContentProvider 初始化，尝试 updateAdConfig 强制覆盖
        if (!initResult || !TTAdSdk.isSdkReady()) {
            TTAdSdk.updateAdConfig(config)
            android.util.Log.i("CsjAdProvider", "TTAdSdk.updateAdConfig called, isSdkReady=${TTAdSdk.isSdkReady()}")
        }
        initialized = true
    }

    // ── 第二步：ensureReady（start + 轮询 + 线程安全，对齐 flutter_merge） ──

    private suspend fun ensureReady(): Result<Unit> {
        // 已就绪 → 直接返回
        if (started && isReady()) {
            return Result.success(Unit)
        }
        // 正在启动中 → 加入等待
        synchronized(this) {
            if (starting) {
                return@synchronized // 走外部 suspendCancellableCoroutine 轮询
            }
            if (started && isReady()) {
                return Result.success(Unit)
            }
            starting = true
        }

        return suspendCancellableCoroutine { cont ->
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    synchronized(this@CsjAdProvider) {
                        started = true
                        starting = false
                    }
                    if (cont.isActive) waitForSdkReady(cont, 0)
                }
                override fun fail(code: Int, msg: String) {
                    synchronized(this@CsjAdProvider) {
                        started = false
                        starting = false
                    }
                    if (cont.isActive) {
                        cont.resume(Result.failure(Exception("CSJ start failed[$code]: $msg")))
                    }
                }
            })
        }
    }

    // ── SDK 状态检查 ──

    override fun isReady(): Boolean {
        return try {
            TTAdSdk.isSdkReady()
        } catch (_: Exception) { false }
    }

    // ── 轮询 isSdkReady（对齐 flutter_merge waitForReady：20 次×120ms = 2.4s） ──

    private fun waitForSdkReady(
        cont: kotlinx.coroutines.CancellableContinuation<Result<Unit>>,
        tries: Int,
    ) {
        if (isReady()) {
            synchronized(this@CsjAdProvider) {
                started = true
                starting = false
            }
            android.util.Log.e("CsjAdProvider", "SDK ready at try=$tries")
            if (cont.isActive) cont.resume(Result.success(Unit))
            return
        }
        if (tries >= 20) {
            synchronized(this@CsjAdProvider) {
                starting = false
            }
            android.util.Log.e("CsjAdProvider", "SDK ready TIMEOUT after $tries tries")
            if (cont.isActive) cont.resume(Result.failure(Exception("CSJ SDK ready timeout")))
            return
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (cont.isActive) waitForSdkReady(cont, tries + 1)
        }, 120)
    }

    override fun revokePrivacyConsent(context: Context) {
        // 对齐 flutter_merge：重置所有状态标志
        synchronized(this) {
            initialized = false
            starting = false
            started = false
        }
    }

    override fun loadBanner(codeId: String, activity: Activity?): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val density = ctx.resources.displayMetrics.density
        val screenWidthPx = ctx.resources.displayMetrics.widthPixels
        val widthDp = screenWidthPx / density

        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setImageAcceptedSize(screenWidthPx, (75f * density).toInt())
            .setExpressViewAcceptedSize(widthDp, 75f) // 75dp 标准横幅高度
            .build()

        android.util.Log.e(TAG, "Banner load start: codeId=$codeId")
        val adNative = TTAdSdk.getAdManager().createAdNative(ctx)
        adNative.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(errorCode: Int, errorMsg: String) {
                android.util.Log.e(TAG, "Banner onError[$errorCode]: $errorMsg")
                state.value = AdLoadState.Error("CSJ Banner error[$errorCode]: $errorMsg")
            }

            override fun onNativeExpressAdLoad(ads: MutableList<TTNativeExpressAd>?) {
                if (ads.isNullOrEmpty()) {
                    state.value = AdLoadState.Error("CSJ Banner: 无广告返回")
                    return
                }
                val ad = ads[0]
                ad.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
                    override fun onAdClicked(view: View?, i: Int) {}
                    override fun onAdShow(view: View?, i: Int) {}
                    override fun onRenderFail(view: View?, msg: String?, code: Int) {
                        state.value = AdLoadState.Error("CSJ render fail: $msg")
                    }
                    override fun onRenderSuccess(view: View?, width: Float, height: Float) {
                        val renderView = view ?: ad.expressAdView
                        if (renderView != null) {
                            val container = buildContainer(ctx, renderView, width, height)
                            state.value = AdLoadState.Loaded(container)
                        } else {
                            state.value = AdLoadState.Error("CSJ render view is null")
                        }
                    }
                })
                ad.render()
            }
        })
        return state
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setImageAcceptedSize(ctx.resources.displayMetrics.widthPixels, 0)
            .setAdCount(count.coerceAtLeast(1))
            .setAdLoadType(TTAdLoadType.LOAD)
            .setMediationAdSlot(com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                .setExtraObject("show_adn_load_error_detail", true).build())
            .build()

        val adNative = TTAdSdk.getAdManager().createAdNative(ctx)
        adNative.loadFeedAd(adSlot, object : TTAdNative.FeedAdListener {
            override fun onError(errorCode: Int, errorMsg: String?) {
                state.value = AdLoadState.Error("CSJ Feed error[$errorCode]: $errorMsg")
            }

            override fun onFeedAdLoad(list: MutableList<com.bytedance.sdk.openadsdk.TTFeedAd>?) {
                if (list.isNullOrEmpty()) {
                    state.value = AdLoadState.Error("CSJ Feed: 无广告返回")
                    return
                }
                val views = mutableListOf<View>()
                for (ad in list) {
                    val adView = ad.adView ?: continue
                    if (adView.parent is ViewGroup) {
                        (adView.parent as ViewGroup).removeView(adView)
                    }
                    val container = FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        addView(adView, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        ))
                    }
                    views.add(container)
                    ad.render()
                }
                state.value = if (views.isNotEmpty()) AdLoadState.Loaded(views.toList())
                    else AdLoadState.Error("CSJ Feed: 渲染返回空")
            }
        })
        return state
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (codeId.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setOrientation(TTAdConstant.VERTICAL)
                .setAdLoadType(TTAdLoadType.LOAD)
                .build()

            adNative.loadFullScreenVideoAd(adSlot, object : TTAdNative.FullScreenVideoAdListener {
                private var shown = false

                override fun onError(errorCode: Int, errorMsg: String?) {
                    android.util.Log.w(TAG, "插屏加载失败[$errorCode]: $errorMsg")
                    if (cont.isActive) cont.resume(false)
                }

                override fun onFullScreenVideoAdLoad(ad: com.bytedance.sdk.openadsdk.TTFullScreenVideoAd) {
                    // 加载成功，等待 onFullScreenVideoCached
                }

                override fun onFullScreenVideoCached() {}

                override fun onFullScreenVideoCached(ad: com.bytedance.sdk.openadsdk.TTFullScreenVideoAd) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    // ECPM 日志
                    try {
                        val extra = ad.mediaExtraInfo
                        if (extra != null) {
                            android.util.Log.i(TAG, "【ECPM-CSJ插屏】extra=$extra slotId=$codeId")
                        }
                    } catch (_: Throwable) {}

                    ad.setFullScreenVideoAdInteractionListener(object :
                        com.bytedance.sdk.openadsdk.TTFullScreenVideoAd.FullScreenVideoAdInteractionListener {
                        override fun onAdShow() {
                            shown = true
                            android.util.Log.d(TAG, "插屏 onAdShow")
                        }

                        override fun onAdVideoBarClick() {}

                        override fun onAdClose() {
                            if (cont.isActive) cont.resume(shown)
                        }

                        override fun onVideoComplete() {}

                        override fun onSkippedVideo() {}
                    })

                    try {
                        ad.showFullScreenVideoAd(activity)
                    } catch (t: Throwable) {
                        android.util.Log.e(TAG, "插屏展示失败", t)
                        if (cont.isActive) cont.resume(false)
                    }
                }
            })
        }
    }

    // ── 激励视频 ──
    // 参考 flutter_merge CsjReadRewardVideoActivity:
    // loadRewardVideoAd → onRewardVideoCached(ad) → ad.showRewardVideoAd(activity)
    override suspend fun showRewardVideo(
        activity: Activity, codeId: String, slotKey: String,
    ): RewardResult {
        if (activity.isFinishing || activity.isDestroyed) {
            return RewardResult(finished = true, errorMessage = "activity_dead")
        }
        if (codeId.isEmpty()) {
            return RewardResult(finished = true, errorMessage = "invalid_slot")
        }

        return suspendCancellableCoroutine { cont ->
            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            val rewardName = when (slotKey) {
                "taskReward" -> "任务"
                "pureReward" -> "纯净"
                "downloadQuotaReward" -> "下载"
                else -> "阅读"
            }

            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setUserID(tokenRepo.cachedUserId?.toString() ?: "")
                .setRewardName(rewardName)
                .setOrientation(TTAdConstant.VERTICAL)
                .setAdLoadType(TTAdLoadType.LOAD)
                .build()

            var adRef: TTRewardVideoAd? = null
            var shown = false
            var rewardGranted = false

            // 30 秒超时兜底
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (cont.isActive) {
                    cont.resume(RewardResult(finished = true, errorMessage = "timeout"))
                }
            }
            mainHandler.postDelayed(timeoutRunnable, REWARD_TIMEOUT_MS)

            cont.invokeOnCancellation {
                mainHandler.removeCallbacks(timeoutRunnable)
                adRef = null
            }

            /** 对齐 flutter_merge handleAd：设置监听器 */
            fun handleAd(ad: TTRewardVideoAd) {
                if (adRef != null) return // 已处理过
                adRef = ad
                try {
                    val extra = ad.mediaExtraInfo
                    if (extra != null) android.util.Log.i(TAG, "【ECPM-CSJ激励】extra=$extra slotId=$codeId")
                } catch (_: Throwable) {}

                ad.setRewardAdInteractionListener(object : TTRewardVideoAd.RewardAdInteractionListener {
                    override fun onAdShow() { shown = true }
                    override fun onAdVideoBarClick() {}
                    override fun onAdClose() {
                        android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(timeoutRunnable)
                        if (cont.isActive) cont.resume(RewardResult(
                            shown = shown, finished = true,
                            rewardGranted = rewardGranted, rewardType = slotKey,
                        ))
                    }
                    override fun onVideoComplete() {}
                    override fun onVideoError() {
                        android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(timeoutRunnable)
                        if (cont.isActive) cont.resume(RewardResult(
                            shown = shown, finished = true, errorMessage = "video_error",
                        ))
                    }
                    override fun onRewardVerify(rewardVerify: Boolean, rewardAmount: Int, rewardName: String?, errorCode: Int, errorMsg: String?) {}
                    override fun onRewardArrived(isRewardValid: Boolean, rewardType: Int, extraInfo: android.os.Bundle?) {
                        rewardGranted = isRewardValid
                    }
                    override fun onSkippedVideo() {}
                })
            }

            android.util.Log.e(TAG, "Reward load start: codeId=$codeId slotKey=$slotKey")
            adNative.loadRewardVideoAd(adSlot, object : TTAdNative.RewardVideoAdListener {
                override fun onError(code: Int, message: String?) {
                    android.util.Log.e(TAG, "Reward onError[$code]: $message")
                    android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(timeoutRunnable)
                    if (cont.isActive) cont.resume(RewardResult(
                        finished = true, errorMessage = "load_failed[$code]: ${message ?: ""}",
                    ))
                }

                override fun onRewardVideoAdLoad(ad: TTRewardVideoAd) {
                    android.util.Log.e(TAG, "Reward onRewardVideoAdLoad")
                    handleAd(ad)
                    // GroMore 聚合场景下 onRewardVideoCached 可能不触发，直接 show
                    if (adRef != null && !activity.isFinishing && !activity.isDestroyed) {
                        try {
                            adRef!!.showRewardVideoAd(activity)
                        } catch (t: Throwable) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            if (cont.isActive) cont.resume(RewardResult(
                                finished = true, errorMessage = "show_failed: ${t.message}",
                            ))
                        }
                    }
                }

                override fun onRewardVideoCached() {}

                override fun onRewardVideoCached(ad: TTRewardVideoAd) {
                    android.util.Log.e(TAG, "Reward onRewardVideoCached")
                    handleAd(ad)
                    if (adRef != null && !activity.isFinishing && !activity.isDestroyed) {
                        try {
                            adRef!!.showRewardVideoAd(activity)
                        } catch (t: Throwable) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            if (cont.isActive) cont.resume(RewardResult(
                                finished = true, errorMessage = "show_failed: ${t.message}",
                            ))
                        }
                    }
                }
            })
        }
    }

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return suspendCancellableCoroutine { cont ->
            val container = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            val decorView = activity.window.decorView as? ViewGroup ?: run {
                cont.resume(false); return@suspendCancellableCoroutine
            }

            val dm = activity.resources.displayMetrics
            val screenW = dm.widthPixels
            val screenH = dm.heightPixels
            // SDK 7.5.x+: 仅需 setExpressViewAcceptedSize(dp)
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setExpressViewAcceptedSize(screenW / dm.density, screenH / dm.density)
                .setMediationAdSlot(
                    com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                        .apply {
                            val fallback = CsjConfig.buildSplashFallback()
                            if (fallback != null) {
                                setMediationSplashRequestInfo(fallback)
                            }
                            setExtraObject("show_adn_load_error_detail", true)
                        }
                        .build()
                )
                .build()

            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            cont.invokeOnCancellation {
                try { decorView.removeView(container) } catch (_: Exception) {}
            }

            var loadSuccessReceived = false

            adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                    loadSuccessReceived = true
                    // 等待 onSplashRenderSuccess
                }

                override fun onSplashLoadFail(error: CSJAdError) {
                    // GroMore 瀑布流中单个 ADN 失败不意味着整体失败，
                    // 如果已有其他 ADN 加载成功，等待其渲染
                    android.util.Log.e(TAG, "开屏加载失败: code=${error.code}, msg=${error.msg}")
                    if (!loadSuccessReceived && cont.isActive) {
                        cont.resume(false)
                    }
                }

                override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return
                    ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
                        override fun onSplashAdShow(ad: CSJSplashAd) {}

                        override fun onSplashAdClick(ad: CSJSplashAd) {}

                        override fun onSplashAdClose(ad: CSJSplashAd, closeType: Int) {
                            try { decorView.removeView(container) } catch (_: Exception) {}
                            if (cont.isActive) cont.resume(true)
                        }
                    })
                    decorView.post {
                        decorView.addView(container)
                        ad.showSplashView(container)
                    }
                }

                override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                    android.util.Log.e(TAG, "开屏渲染失败: code=${error.code}, msg=${error.msg}")
                    try { decorView.removeView(container) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(false)
                }
            }, SPLASH_TIMEOUT_MS)
        }
    }

    // ── 工具 ──

    private fun buildContainer(ctx: Context, adView: View, adWidth: Float, adHeight: Float): FrameLayout {
        return FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val scale = ctx.resources.displayMetrics.density
            val w = (adWidth * scale).toInt().coerceAtMost(ctx.resources.displayMetrics.widthPixels)
            val h = (adHeight * scale).toInt()
            val lp = FrameLayout.LayoutParams(w, h, Gravity.CENTER)
            addView(adView, lp)
        }
    }

    companion object {
        private const val SPLASH_TIMEOUT_MS = 5000
        private const val REWARD_TIMEOUT_MS = 30_000L
        private const val GRO_MORE_DELAY_MS = 3000L  // isSdkReady 后等 GroMore 配置下载
        private const val TAG = "CsjAdProvider"
    }
}
