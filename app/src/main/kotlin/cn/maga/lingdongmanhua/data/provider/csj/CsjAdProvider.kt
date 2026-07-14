package cn.maga.lingdongmanhua.data.provider.csj

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
import cn.maga.lingdongmanhua.core.AppContextHolder
import cn.maga.lingdongmanhua.domain.model.AdLoadState
import cn.maga.lingdongmanhua.domain.model.RewardResult
import cn.maga.lingdongmanhua.domain.provider.AdProvider
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import com.bytedance.sdk.openadsdk.TTFeedAd
import com.bytedance.sdk.openadsdk.TTNativeAd
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CsjAdProvider(
    private val tokenRepo: cn.maga.lingdongmanhua.domain.repository.TokenRepository,
) : AdProvider {

    @Volatile private var initialized = false
    @Volatile private var starting = false
    @Volatile private var started = false

    override suspend fun initialize(context: Context): Result<Unit> {
        initIfNeeded(context)
        return ensureReady()
    }

    private fun initIfNeeded(context: Context) {
        if (initialized) return
        val config = CsjConfig.buildAdConfig(context)
        val initResult = TTAdSdk.init(context.applicationContext, config)
        android.util.Log.i("CsjAdProvider", "TTAdSdk.init result=$initResult appId=${CsjConfig.APP_ID} isSdkReady=${TTAdSdk.isSdkReady()}")
        initialized = true
    }

    private suspend fun ensureReady(): Result<Unit> {
        if (started && isReady()) return Result.success(Unit)

        val shouldStart = synchronized(this) {
            if (starting || (started && isReady())) false
            else { starting = true; true }
        }
        if (!shouldStart) {
            var waited = 0
            while (waited < 3000 && !isReady()) {
                delay(50)
                waited += 50
            }
            return if (isReady()) Result.success(Unit)
                   else Result.failure(Exception("CSJ SDK ready timeout"))
        }

        return suspendCancellableCoroutine { cont ->
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    synchronized(this@CsjAdProvider) { started = true; starting = false }
                    if (cont.isActive) waitForSdkReady(cont, 0)
                }
                override fun fail(code: Int, msg: String) {
                    synchronized(this@CsjAdProvider) { started = false; starting = false }
                    if (cont.isActive) cont.resume(Result.failure(Exception("CSJ start failed[$code]: $msg")))
                }
            })
        }
    }

    override fun isReady(): Boolean {
        return try {
            TTAdSdk.isSdkReady()
        } catch (_: Exception) { false }
    }

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

        container.post {
            val cw = container.width
            val ch = container.height
            val wPx = if (cw > 0) cw else ctx.resources.displayMetrics.widthPixels
            val hPx = Math.max(1, Math.round((if (ch > 0) ch / density else 75f) * density))
            val widthDp = wPx / density
            val heightDp = hPx.toFloat() / density

            // GroMore 聚合模式：setMediationAdSlot
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setImageAcceptedSize(wPx, hPx)
                .setExpressViewAcceptedSize(widthDp, heightDp)
                .setMediationAdSlot(MediationAdSlot.Builder().build())
                .build()

            android.util.Log.e(TAG, "Banner load(GroMore): codeId=$codeId cw=$cw ch=$ch wDp=$widthDp hDp=$heightDp")
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
        val container = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (420f * density).toInt(),
            )
            setPadding(0, 0, 0, 0)
            setClipChildren(true)
            setClipToPadding(true)
        }
        state.value = AdLoadState.Loaded(listOf(container))

        container.post {
            val screenW = if (container.width > 0) container.width
                else ctx.resources.displayMetrics.widthPixels
            // GroMore 聚合模式：setMediationAdSlot
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setImageAcceptedSize(Math.max(1, screenW), 0)
                .setAdCount(3)
                .setMediationAdSlot(MediationAdSlot.Builder().build())
                .build()

            android.util.Log.e(TAG, "Feed load(GroMore): codeId=$codeId sw=$screenW")
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

            // GroMore 聚合模式：setMediationAdSlot
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setOrientation(TTAdConstant.VERTICAL)
                .setAdLoadType(TTAdLoadType.LOAD)
                .setMediationAdSlot(MediationAdSlot.Builder().build())
                .build()

            adNative.loadFullScreenVideoAd(adSlot, object : TTAdNative.FullScreenVideoAdListener {
                private var shown = false

                override fun onError(errorCode: Int, errorMsg: String?) {
                    android.util.Log.w(TAG, "插屏加载失败[$errorCode]: $errorMsg")
                    if (cont.isActive) cont.resume(false)
                }

                override fun onFullScreenVideoAdLoad(ad: com.bytedance.sdk.openadsdk.TTFullScreenVideoAd) {}

                override fun onFullScreenVideoCached() {}

                override fun onFullScreenVideoCached(ad: com.bytedance.sdk.openadsdk.TTFullScreenVideoAd) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

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

            // GroMore 聚合模式：setMediationAdSlot
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setUserID(tokenRepo.cachedUserId?.toString() ?: "")
                .setRewardName(rewardName)
                .setOrientation(TTAdConstant.VERTICAL)
                .setAdLoadType(TTAdLoadType.LOAD)
                .setMediationAdSlot(MediationAdSlot.Builder().build())
                .build()

            var adRef: TTRewardVideoAd? = null
            var shown = false
            var rewardGranted = false

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

            fun handleAd(ad: TTRewardVideoAd) {
                if (adRef != null) return
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

            android.util.Log.e(TAG, "Reward load start(GroMore): codeId=$codeId slotKey=$slotKey")
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

    // ── 开屏广告（GroMore 聚合模式）──

    /** GroMore 开屏请求信息（对齐 flutter_merge: ADN_PANGLE, 全空字符串） */
    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    override suspend fun showSplashAd(
        activity: Activity, codeId: String, container: ViewGroup,
        onAdLoaded: (() -> Unit)?, onAdShown: (() -> Unit)?,
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return suspendCancellableCoroutine { cont ->
            val dm = activity.resources.displayMetrics
            val screenW = dm.widthPixels
            val screenH = dm.heightPixels

            // GroMore 聚合模式：setMediationAdSlot + MediationSplashRequestInfo
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setExpressViewAcceptedSize(screenW / dm.density, screenH / dm.density)
                .setMediationAdSlot(
                    MediationAdSlot.Builder()
                        .setMediationSplashRequestInfo(csjSplashRequestInfo)
                        .build()
                )
                .build()

            android.util.Log.i(TAG, "开屏广告(GroMore模式): codeId=$codeId")

            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            cont.invokeOnCancellation {
                try { container.removeAllViews() } catch (_: Exception) {}
            }

            var loadSuccessReceived = false

            adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                    loadSuccessReceived = true
                }

                override fun onSplashLoadFail(error: CSJAdError) {
                    android.util.Log.e(TAG, "开屏加载失败: code=${error.code}, msg=${error.msg}")
                    if (!loadSuccessReceived && cont.isActive) {
                        cont.resume(false)
                    }
                }

                override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    onAdLoaded?.invoke()
                    android.util.Log.i(TAG, "开屏广告素材就绪，等待容器…")

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

    companion object {
        private const val SPLASH_TIMEOUT_MS = 10_000
        private const val REWARD_TIMEOUT_MS = 30_000L
        private const val TAG = "CsjAdProvider"
    }
}
