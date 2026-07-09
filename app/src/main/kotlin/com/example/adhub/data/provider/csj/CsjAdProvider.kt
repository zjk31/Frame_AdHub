package com.example.adhub.data.provider.csj

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CsjAdProvider(
    private val tokenRepo: com.example.adhub.domain.repository.TokenRepository,
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
        val config = TTAdConfig.Builder()
            .appId(CsjConfig.APP_ID)
            .appName("Frame_AdHub")
            .useMediation(true)
            .debug(true)
            .supportMultiProcess(false)
            .customController(object : TTCustomController() {
                override fun isCanUseLocation(): Boolean = true
                override fun isCanUsePhoneState(): Boolean = true
                override fun isCanUseWifiState(): Boolean = true
                override fun isCanUseWriteExternal(): Boolean = true
                override fun isCanUseAndroidId(): Boolean = true
                override fun alist(): Boolean = false
                override fun getMediationPrivacyConfig(): MediationPrivacyConfig {
                    return object : MediationPrivacyConfig() {
                        override fun isLimitPersonalAds(): Boolean = false
                        override fun isProgrammaticRecommend(): Boolean = true
                    }
                }
            })
            .build()
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
            if (cont.isActive) cont.resume(Result.success(Unit))
            return
        }
        if (tries >= 20) {
            synchronized(this@CsjAdProvider) {
                starting = false
            }
            if (cont.isActive) {
                cont.resume(Result.failure(Exception("CSJ SDK ready timeout after start")))
            }
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

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val density = ctx.resources.displayMetrics.density
        val screenWidthPx = ctx.resources.displayMetrics.widthPixels
        val widthDp = screenWidthPx / density
        val heightDp = 75f

        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setImageAcceptedSize(screenWidthPx, (heightDp * density).toInt())
            .setExpressViewAcceptedSize(widthDp, heightDp)
            .build()

        val adNative = TTAdSdk.getAdManager().createAdNative(ctx)
        adNative.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(errorCode: Int, errorMsg: String) {
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

            cont.invokeOnCancellation {
                adRef = null
            }

            adNative.loadRewardVideoAd(adSlot, object : TTAdNative.RewardVideoAdListener {
                override fun onError(code: Int, message: String?) {
                    if (cont.isActive) {
                        cont.resume(RewardResult(
                            finished = true,
                            errorMessage = "load_failed[$code]: ${message ?: ""}",
                        ))
                    }
                }

                override fun onRewardVideoAdLoad(ad: TTRewardVideoAd) {
                    // 仅加载成功，等待缓存
                }

                override fun onRewardVideoCached() {
                    // 空实现
                }

                override fun onRewardVideoCached(ad: TTRewardVideoAd) {
                    // 缓存完成 → 自动展示
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    adRef = ad
                    var shown = false
                    var rewardGranted = false

                    // ECPM 日志
                    try {
                        val extra = ad.mediaExtraInfo
                        if (extra != null) {
                            android.util.Log.i(TAG, "【ECPM-CSJ激励】extra=$extra slotId=$codeId")
                        }
                    } catch (_: Throwable) {}

                    ad.setRewardAdInteractionListener(object : TTRewardVideoAd.RewardAdInteractionListener {
                        override fun onAdShow() {
                            shown = true
                        }

                        override fun onAdVideoBarClick() {}

                        override fun onAdClose() {
                            if (cont.isActive) {
                                cont.resume(RewardResult(
                                    shown = shown,
                                    finished = true,
                                    rewardGranted = rewardGranted,
                                    rewardType = slotKey,
                                ))
                            }
                        }

                        override fun onVideoComplete() {}

                        override fun onVideoError() {
                            if (cont.isActive) {
                                cont.resume(RewardResult(
                                    shown = shown,
                                    finished = true,
                                    errorMessage = "video_error",
                                ))
                            }
                        }

                        override fun onRewardVerify(
                            rewardVerify: Boolean,
                            rewardAmount: Int,
                            rewardName: String?,
                            errorCode: Int,
                            errorMsg: String?,
                        ) {
                            // 旧版回调，一般不触发
                        }

                        override fun onRewardArrived(
                            isRewardValid: Boolean,
                            rewardType: Int,
                            extraInfo: android.os.Bundle?,
                        ) {
                            rewardGranted = isRewardValid
                        }

                        override fun onSkippedVideo() {}
                    })

                    try {
                        ad.showRewardVideoAd(activity)
                    } catch (t: Throwable) {
                        if (cont.isActive) {
                            cont.resume(RewardResult(
                                finished = true,
                                errorMessage = "show_failed: ${t.message}",
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
                setBackgroundColor(Color.WHITE)
            }
            val decorView = activity.window.decorView as? ViewGroup ?: run {
                cont.resume(false); return@suspendCancellableCoroutine
            }

            val dm = activity.resources.displayMetrics
            val adSlot = AdSlot.Builder()
                .setCodeId(codeId)
                .setExpressViewAcceptedSize(dm.widthPixels / dm.density, dm.heightPixels / dm.density)
                .build()

            val adNative = TTAdSdk.getAdManager().createAdNative(activity)

            cont.invokeOnCancellation {
                try { decorView.removeView(container) } catch (_: Exception) {}
            }

            adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                    // 等待 onSplashRenderSuccess
                }

                override fun onSplashLoadFail(error: CSJAdError) {
                    if (cont.isActive) cont.resume(false)
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
        private const val TAG = "CsjAdProvider"
    }
}
