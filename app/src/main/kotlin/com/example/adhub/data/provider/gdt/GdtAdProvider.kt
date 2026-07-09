package com.example.adhub.data.provider.gdt

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import com.qq.e.ads.banner2.UnifiedBannerADListener
import com.qq.e.ads.banner2.UnifiedBannerView
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener
import com.qq.e.ads.rewardvideo.RewardVideoAD
import com.qq.e.ads.rewardvideo.RewardVideoADListener
import com.qq.e.ads.splash.SplashAD
import com.qq.e.ads.splash.SplashADListener
import com.qq.e.comm.managers.GDTAdSdk
import com.qq.e.comm.util.AdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GdtAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        return try {
            // 优量汇 SDK 初始化
            // 参考: https://developers.qq.com/doc/GDT/sdk/android/SDKAPI
            GDTAdSdk.init(context, GdtConfig.APP_ID)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = true

    override fun revokePrivacyConsent(context: Context) {
        // GDT 隐私合规：通过 GDTAdSdk 重启实现
        GDTAdSdk.init(context, GdtConfig.APP_ID)
    }

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val activity = ctx as? Activity ?: run {
            state.value = AdLoadState.Error("GDT: Context is not Activity")
            return state
        }

        lateinit var bannerView: UnifiedBannerView
        bannerView = UnifiedBannerView(activity, codeId, object : UnifiedBannerADListener {
            override fun onADReceive() {
                val container = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    val screenW = ctx.resources.displayMetrics.widthPixels
                    val bannerH = (screenW / 6.4f).toInt()
                    val lp = FrameLayout.LayoutParams(screenW, bannerH, Gravity.CENTER_HORIZONTAL)
                    addView(bannerView, lp)
                }
                bannerView.setRefresh(30)
                state.value = AdLoadState.Loaded(container)
            }

            override fun onNoAD(error: AdError) {
                state.value = AdLoadState.Error("GDT no ad: ${error.errorMsg}")
            }

            override fun onADExposure() {}
            override fun onADClosed() {}
            override fun onADClicked() {}
            override fun onADLeftApplication() {}
        })

        bannerView.loadAD()
        return state
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val adSize = com.qq.e.ads.nativ.ADSize(com.qq.e.ads.nativ.ADSize.FULL_WIDTH, com.qq.e.ads.nativ.ADSize.AUTO_HEIGHT)

        val expressAD = com.qq.e.ads.nativ.NativeExpressAD(ctx, adSize, codeId, object : com.qq.e.ads.nativ.NativeExpressAD.NativeExpressADListener {
            override fun onADLoaded(adList: MutableList<com.qq.e.ads.nativ.NativeExpressADView>?) {
                if (adList.isNullOrEmpty()) {
                    state.value = AdLoadState.Error("GDT Feed: 无广告返回")
                    return
                }

                val views = adList.map { adView ->
                    // ECPM 日志
                    android.util.Log.i(TAG, "【ECPM-GDT信息流】ecpm=${adView.ecpm} level=${adView.ecpmLevel}")

                    val container = FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    }
                    if (adView.parent is ViewGroup) {
                        (adView.parent as ViewGroup).removeView(adView)
                    }
                    container.addView(adView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ))
                    adView.render()
                    container
                }
                state.value = AdLoadState.Loaded(views)
            }

            override fun onRenderFail(adView: com.qq.e.ads.nativ.NativeExpressADView?) {
                state.value = AdLoadState.Error("GDT Feed render fail")
            }

            override fun onRenderSuccess(adView: com.qq.e.ads.nativ.NativeExpressADView?) {}

            override fun onADExposure(adView: com.qq.e.ads.nativ.NativeExpressADView?) {}
            override fun onADClicked(adView: com.qq.e.ads.nativ.NativeExpressADView?) {}
            override fun onADClosed(adView: com.qq.e.ads.nativ.NativeExpressADView?) {}

            override fun onNoAD(error: com.qq.e.comm.util.AdError?) {
                state.value = AdLoadState.Error("GDT Feed no ad: ${error?.errorCode} ${error?.errorMsg}")
            }

            override fun onADLeftApplication(adView: com.qq.e.ads.nativ.NativeExpressADView?) {}
        })
        expressAD.loadAD(count.coerceAtLeast(1))
        return state
    }

    // ── 插屏 ──
    // 参考 flutter_merge GdtUnifiedInterstitialPlatformView:
    // UnifiedInterstitialAD(posId, listener) → loadAD() → onADReceive → show()
    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (codeId.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            var shown = false
            var loadSuccess = false
            var renderFail = false
            val adRef = arrayOf<UnifiedInterstitialAD?>(null)

            fun destroyAd() {
                try { adRef[0]?.destroy() } catch (_: Throwable) {}
                adRef[0] = null
            }

            fun tryShow() {
                if (adRef[0] == null || shown || !loadSuccess || renderFail) return
                if (!adRef[0]!!.isValid) return
                try {
                    adRef[0]!!.show()
                    shown = true
                } catch (t: Throwable) {
                    destroyAd()
                    android.util.Log.e(TAG, "插屏展示失败", t)
                    if (cont.isActive) cont.resume(false)
                }
            }

            val listener = object : UnifiedInterstitialADListener {
                override fun onADReceive() {
                    loadSuccess = true
                    try {
                        val ecpm = adRef[0]?.ecpm ?: -1
                        val level = adRef[0]?.ecpmLevel ?: ""
                        android.util.Log.i(TAG, "【ECPM-GDT插屏】ecpm=$ecpm level=$level posId=$codeId")
                    } catch (_: Throwable) {}
                    tryShow()
                }

                override fun onNoAD(error: AdError) {
                    if (cont.isActive) cont.resume(false)
                }

                override fun onADOpened() {}

                override fun onVideoCached() {}

                override fun onADExposure() {
                    shown = true
                }

                override fun onADClicked() {}

                override fun onADLeftApplication() {}

                override fun onADClosed() {
                    destroyAd()
                    if (cont.isActive) cont.resume(shown)
                }

                override fun onRenderSuccess() {
                    tryShow()
                }

                override fun onRenderFail() {
                    renderFail = true
                    destroyAd()
                    if (cont.isActive) cont.resume(false)
                }
            }

            val ad = UnifiedInterstitialAD(activity, codeId, listener)
            adRef[0] = ad
            ad.loadAD()
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
            var shown = false
            var rewardGranted = false
            var adRef: RewardVideoAD? = null

            val listener = object : RewardVideoADListener {
                override fun onADLoad() {
                    val ad = adRef ?: return
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    // ECPM 日志
                    try {
                        android.util.Log.i(TAG, "【ECPM-GDT激励】ecpm=${ad.ecpm} level=${ad.ecpmLevel} posId=$codeId")
                    } catch (_: Throwable) {}

                    if (!ad.isValid) {
                        adRef = null
                        if (cont.isActive) {
                            cont.resume(RewardResult(finished = true, errorMessage = "invalid after load"))
                        }
                        return
                    }

                    try {
                        ad.showAD(activity)
                    } catch (t: Throwable) {
                        adRef = null
                        if (cont.isActive) {
                            cont.resume(RewardResult(finished = true, errorMessage = "show_failed: ${t.message}"))
                        }
                    }
                }

                override fun onVideoCached() {}

                override fun onADShow() {}

                override fun onADExpose() {
                    shown = true
                }

                override fun onReward(map: Map<String, Any>?) {
                    rewardGranted = true
                }

                override fun onADClick() {}

                override fun onVideoComplete() {}

                override fun onADClose() {
                    adRef = null
                    if (cont.isActive) {
                        cont.resume(RewardResult(
                            shown = shown,
                            finished = true,
                            rewardGranted = rewardGranted,
                            rewardType = slotKey,
                        ))
                    }
                }

                override fun onError(adError: AdError) {
                    adRef = null
                    if (cont.isActive) {
                        val detail = "${adError.errorCode}: ${adError.errorMsg}"
                        cont.resume(RewardResult(
                            shown = shown,
                            finished = true,
                            errorMessage = detail,
                        ))
                    }
                }
            }

            // true = 竜屏
            val ad = RewardVideoAD(activity, codeId, listener, true)
            adRef = ad
            ad.loadAD()
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

            var splashAd: SplashAD? = null

            cont.invokeOnCancellation {
                try { decorView.removeView(container) } catch (_: Exception) {}
            }

            splashAd = SplashAD(activity, codeId, object : SplashADListener {
                override fun onADPresent() {}
                override fun onADClicked() {}
                override fun onADTick(millisUntilFinished: Long) {}
                override fun onADExposure() {}

                override fun onADLoaded(expireTimestamp: Long) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return
                    decorView.post {
                        decorView.addView(container)
                        splashAd?.showAd(container)
                    }
                }

                override fun onNoAD(error: AdError) {
                    if (cont.isActive) cont.resume(false)
                }

                override fun onADDismissed() {
                    try { decorView.removeView(container) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(true)
                }
            }, SPLASH_TIMEOUT_MS)
            splashAd.fetchAdOnly()
        }
    }

    companion object {
        private const val SPLASH_TIMEOUT_MS = 5000
        private const val TAG = "GdtAdProvider"
    }
}
