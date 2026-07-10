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
import com.bytedance.sdk.openadsdk.TTFeedAd
import com.bytedance.sdk.openadsdk.TTNativeAd
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

        // 容器：对齐 flutter_merge MATCH_PARENT x MATCH_PARENT + clipChildren
        val container = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setPadding(0, 0, 0, 0)
            setClipChildren(true)
            setClipToPadding(true)
        }
        state.value = AdLoadState.Loaded(container)

        // 等容器测量后再加载广告，避免宽高为 0
        container.post {
            val cw = container.width
            val ch = container.height
            val wPx = if (cw > 0) cw else ctx.resources.displayMetrics.widthPixels
            val hPx = Math.max(1, Math.round((if (ch > 0) ch / density else 75f) * density))
            val widthDp = wPx / density
            val heightDp = hPx.toFloat() / density

            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setImageAcceptedSize(wPx, hPx)
                .setExpressViewAcceptedSize(widthDp, heightDp)
                .setMediationAdSlot(
                    com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                        .setExtraObject("show_adn_load_error_detail", true)
                        .build()
                )
                .build()

            android.util.Log.e(TAG, "Banner load: codeId=$codeId cw=$cw ch=$ch wDp=$widthDp hDp=$heightDp")
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
                            android.util.Log.e(TAG, "Banner onRenderFail: msg=$msg code=$code")
                        }
                        override fun onRenderSuccess(view: View?, width: Float, height: Float) {
                            val renderView = view ?: ad.expressAdView
                            if (renderView == null) return
                            renderView.setPadding(0, 0, 0, 0)
                            container.post {
                                container.removeAllViews()
                                val rw = if (width > 0f) width.toInt() else 0
                                val rh = if (height > 0f) height.toInt() else 0
                                val lp = when {
                                    cw > 0 && ch > 0 && rw > 0 && rh > 0 && (rw > cw || rh > ch) -> {
                                        val scale = minOf(cw.toFloat() / rw, ch.toFloat() / rh)
                                        FrameLayout.LayoutParams(
                                            maxOf(1, (rw * scale).toInt()),
                                            maxOf(1, (rh * scale).toInt()),
                                            Gravity.CENTER,
                                        )
                                    }
                                    rw > 0 && rh > 0 && cw > 0 && ch > 0 ->
                                        FrameLayout.LayoutParams(rw, rh, Gravity.CENTER)
                                    else -> FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                }
                                container.addView(renderView, lp)
                            }
                        }
                    })
                    ad.render()
                }
            })
        }
        return state
    }
    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val density = ctx.resources.displayMetrics.density
        // 对齐 flutter_merge: rootView 始终在视图树，先返回容器让 Compose 挂载
        val container = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (420f * density).toInt(),  // 固定高度 420dp
            )
            setPadding(0, 0, 0, 0)
            setClipChildren(true)
            setClipToPadding(true)
        }
        // 立即把容器发出去，Compose AndroidView 才能挂载它
        state.value = AdLoadState.Loaded(listOf(container))

        // container.post: 挂载后执行加载，此时 container.width 有值
        container.post {
            val screenW = if (container.width > 0) container.width
                else ctx.resources.displayMetrics.widthPixels
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setImageAcceptedSize(Math.max(1, screenW), 0)
                .setAdCount(3)
                .setMediationAdSlot(
                    com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                        .setMuted(false)
                        .build()
                )
                .build()

            android.util.Log.e(TAG, "Feed load: codeId=$codeId sw=$screenW")
            val adNative = TTAdSdk.getAdManager().createAdNative(ctx)
            adNative.loadFeedAd(adSlot, object : TTAdNative.FeedAdListener {
                override fun onError(errorCode: Int, errorMsg: String?) {
                    android.util.Log.e(TAG, "Feed onError[$errorCode]: $errorMsg")
                    state.value = AdLoadState.Error("CSJ Feed error[$errorCode]: $errorMsg")
                }

                override fun onFeedAdLoad(list: MutableList<TTFeedAd>?) {
                    if (list.isNullOrEmpty()) {
                        state.value = AdLoadState.Error("CSJ Feed: 无广告返回")
                        return
                    }
                    val ad = list[0]
                    ad.setExpressRenderListener(object : TTNativeAd.ExpressRenderListener {
                        override fun onRenderSuccess(view: View?, width: Float, height: Float, isExpress: Boolean) {
                            val adView = ad.adView ?: view ?: return
                            adView.setPadding(0, 0, 0, 0)
                            container.removeAllViews()
                            container.addView(adView, FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ))
                            state.value = AdLoadState.Loaded(listOf(container))
                            android.util.Log.e(TAG, "Feed render: w=$width h=$height")
                        }
                    })
                    ad.render()
                }
            })
        }
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
                .setMediationAdSlot(
                    com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                        .setExtraObject("show_adn_load_error_detail", true)
                        .build()
                )
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

    // ── 开屏广告（对齐 flutter_merge 预加载模式） ──
    //
    // 1. loadSplashAd → onSplashRenderSuccess 存 ad，回调 onAdLoaded（释放系统 splash）
    // 2. 等待 container 可见后 showSplashView → onSplashAdClose → 恢复协程
    override suspend fun showSplashAd(
        activity: Activity, codeId: String, container: ViewGroup,
        onAdLoaded: (() -> Unit)?, onAdShown: (() -> Unit)?,
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return suspendCancellableCoroutine { cont ->
            val dm = activity.resources.displayMetrics
            val screenW = dm.widthPixels
            val screenH = dm.heightPixels
            // SDK 7.5.x+: 仅需 setExpressViewAcceptedSize(dp)
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setExpressViewAcceptedSize(screenW / dm.density, screenH / dm.density)
                .setAdLoadType(TTAdLoadType.PRELOAD)
                .setMediationAdSlot(
                    com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot.Builder()
                        .setMediationSplashRequestInfo(CsjConfig.buildSplashFallback())
                        .setExtraObject("show_adn_load_error_detail", true)
                        .build()
                )
                .build()

            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            cont.invokeOnCancellation {
                try { container.removeAllViews() } catch (_: Exception) {}
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

                    // ① 暂存广告引用（对齐 flutter_merge: pendingCsjSplash = ad）
                    // ② 先回调 onAdLoaded，让调用方释放系统 splash
                    // ③ 等容器就绪后再真正展示
                    onAdLoaded?.invoke()
                    android.util.Log.i(TAG, "开屏广告素材就绪，等待容器…")

                    // 延迟一帧展示，确保系统 splash 消退不影响 CSJ 倒计时初始化
                    container.post {
                        if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return@post
                        android.util.Log.i(TAG, "开始展示开屏广告")
                        container.removeAllViews()
                        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
                            override fun onSplashAdShow(ad2: CSJSplashAd) {
                                android.util.Log.i(TAG, "开屏广告已展示")
                                onAdShown?.invoke()
                            }
                            override fun onSplashAdClick(ad2: CSJSplashAd) {
                                android.util.Log.i(TAG, "开屏广告被点击")
                            }
                            override fun onSplashAdClose(ad2: CSJSplashAd, closeType: Int) {
                                android.util.Log.i(TAG, "开屏广告关闭, closeType=$closeType (1=跳过 2=倒计时结束 3=点击落地页)")
                                try { container.removeAllViews() } catch (_: Exception) {}
                                if (cont.isActive) cont.resume(true)
                            }
                        })
                        ad.showSplashView(container)
                    }
                }

                override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                    android.util.Log.e(TAG, "开屏渲染失败: code=${error.code}, msg=${error.msg}")
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
            val w = if (adWidth > 0f) (adWidth * scale).toInt().coerceAtMost(ctx.resources.displayMetrics.widthPixels)
                    else ViewGroup.LayoutParams.WRAP_CONTENT
            val h = if (adHeight > 0f) (adHeight * scale).toInt()
                    else ViewGroup.LayoutParams.WRAP_CONTENT
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
