package cn.android.adhub.data.provider.baidu

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.baidu.mobads.sdk.api.BDAdConfig
import com.baidu.mobads.sdk.api.BaiduNativeManager
import com.baidu.mobads.sdk.api.ExpressInterstitialAd
import com.baidu.mobads.sdk.api.ExpressInterstitialListener
import com.baidu.mobads.sdk.api.ExpressResponse
import com.baidu.mobads.sdk.api.RequestParameters
import com.baidu.mobads.sdk.api.RewardVideoAd
import com.baidu.mobads.sdk.api.SplashAd
import com.baidu.mobads.sdk.api.SplashInteractionListener
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.domain.model.AdLoadState
import cn.android.adhub.domain.model.RewardResult
import cn.android.adhub.domain.provider.AdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BaiduAdProvider(
    private val tokenRepo: cn.android.adhub.domain.repository.TokenRepository,
) : AdProvider {

    private var initialized = false

    override suspend fun initialize(context: Context): Result<Unit> {
        if (initialized) return Result.success(Unit)
        return try {
            // 对齐 flutter_merge: 权限配置
            com.baidu.mobads.sdk.api.MobadsPermissionSettings.setPermissionReadDeviceID(true)
            com.baidu.mobads.sdk.api.MobadsPermissionSettings.setPermissionAppList(false)
            com.baidu.mobads.sdk.api.MobadsPermissionSettings.setPermissionLocation(false)
            com.baidu.mobads.sdk.api.MobadsPermissionSettings.setPermissionStorage(false)

            val config = BDAdConfig.Builder()
                .setAppsid(BaiduConfig.APP_ID)
                .setAppName("Frame_AdHub")
                .setDebug(true)
                .setBDAdInitListener(object : BDAdConfig.BDAdInitListener {
                    override fun success() {
                        initialized = true
                        android.util.Log.i(TAG, "Baidu SDK init success, appId=${BaiduConfig.APP_ID}")
                    }

                    override fun fail() {
                        android.util.Log.e(TAG, "Baidu SDK init failed")
                    }
                })
                .build(context.applicationContext)
            config.init()
            // init 是异步的，等一下
            var retries = 0
            while (!initialized && retries < 20) {
                kotlinx.coroutines.delay(100)
                retries++
            }
            if (initialized) Result.success(Unit)
            else Result.failure(RuntimeException("Baidu SDK init timeout"))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Baidu SDK init failed", e)
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = initialized
    override fun revokePrivacyConsent(context: Context) {}

    override fun loadBanner(codeId: String, activity: Activity?): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val act = activity ?: ctx as? Activity
        if (act == null) {
            state.value = AdLoadState.Error("Baidu: Context is not Activity")
            return state
        }

        val container = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        val loading = ProgressBar(ctx).also {
            container.addView(it, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ))
        }

        val nativeManager = BaiduNativeManager(ctx.applicationContext, codeId)
        nativeManager.loadExpressAd(
            RequestParameters.Builder().setBidFloor(10).build(),
            object : BaiduNativeManager.ExpressAdListener {
                override fun onNativeLoad(responses: MutableList<ExpressResponse>?) {
                    if (responses.isNullOrEmpty()) {
                        state.value = AdLoadState.Error("Baidu: 无广告返回")
                        loading.visibility = View.GONE
                        return
                    }
                    val ad = responses[0]
                    ad.bindInteractionActivity(act)
                    ad.setInteractionListener(object : ExpressResponse.ExpressInteractionListener {
                        override fun onAdClick() {}
                        override fun onAdExposed() {}
                        override fun onAdRenderFail(adView: View?, reason: String?, code: Int) {
                            state.value = AdLoadState.Error("Baidu render fail: $reason")
                            loading.visibility = View.GONE
                        }
                        override fun onAdRenderSuccess(adView: View?, width: Float, height: Float) {
                            val v = ad.expressAdView
                            if (v != null) {
                                container.post {
                                    container.removeView(loading)
                                    container.addView(v, FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ))
                                }
                                state.value = AdLoadState.Loaded(container)
                            } else {
                                state.value = AdLoadState.Error("Baidu expressAdView null")
                            }
                            loading.visibility = View.GONE
                        }
                        override fun onAdUnionClick() {}
                    })
                    ad.render()
                }

                override fun onNativeFail(errorCode: Int, message: String?, r: ExpressResponse?) {
                    state.value = AdLoadState.Error("Baidu fail[$errorCode]: $message")
                    loading.visibility = View.GONE
                }

                override fun onNoAd(code: Int, msg: String?, r: ExpressResponse?) {
                    state.value = AdLoadState.Error("Baidu no ad: $msg")
                    loading.visibility = View.GONE
                }

                override fun onVideoDownloadSuccess() {}
                override fun onVideoDownloadFailed() {}
                override fun onLpClosed() {}
            }
        )
        return state
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val nativeManager = BaiduNativeManager(ctx.applicationContext, codeId)

        val requestParams = RequestParameters.Builder()
            .setBidFloor(10)
            .build()

        nativeManager.loadExpressAd(requestParams, object : BaiduNativeManager.ExpressAdListener {
            override fun onNativeLoad(adList: MutableList<ExpressResponse>?) {
                if (adList.isNullOrEmpty()) {
                    state.value = AdLoadState.Error("百度 Feed: 无广告返回")
                    return
                }

                // 只需第一个
                val ad = adList[0]
                for (i in 1 until adList.size) {
                    try { adList[i].destroy() } catch (_: Throwable) {}
                }

                // ECPM 日志
                android.util.Log.i(TAG, "【ECPM-百度信息流】ecpmLevel=${ad.ecpmLevel}")

                ad.setInteractionListener(object : ExpressResponse.ExpressInteractionListener {
                    override fun onAdClick() {}
                    override fun onAdExposed() {}
                    override fun onAdUnionClick() {}
                    override fun onAdRenderFail(adView: View?, reason: String?, code: Int) {
                        android.util.Log.w(TAG, "百度 Feed render fail: $reason")
                        state.value = AdLoadState.Error("百度 Feed render fail: $reason")
                    }

                    override fun onAdRenderSuccess(adView: View?, width: Float, height: Float) {
                        val express = ad.getExpressAdView() ?: return
                        val container = FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        }
                        if (express.parent is ViewGroup) {
                            (express.parent as ViewGroup).removeView(express)
                        }
                        container.addView(express, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        ))
                        state.value = AdLoadState.Loaded(listOf(container))
                    }
                })
                ad.render()
            }

            override fun onNativeFail(errorCode: Int, message: String?, r: ExpressResponse?) {
                state.value = AdLoadState.Error("百度 Feed fail[$errorCode]: $message")
            }

            override fun onNoAd(code: Int, msg: String?, r: ExpressResponse?) {
                state.value = AdLoadState.Error("百度 Feed no ad: $msg")
            }

            override fun onVideoDownloadSuccess() {}
            override fun onVideoDownloadFailed() {}
            override fun onLpClosed() {}
        })
        return state
    }

    // ── 插屏 ──
    // 参考 flutter_merge BaiduInterstitialController:
    // ExpressInterstitialAd(activity.applicationContext, placeId) → load() → onAdCacheSuccess → show(activity)
    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (codeId.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            var shown = false
            val ad = ExpressInterstitialAd(activity.applicationContext, codeId)

            ad.setLoadListener(object : ExpressInterstitialListener {
                override fun onADLoaded() {}

                override fun onAdClick() {}

                override fun onAdClose() {
                    if (cont.isActive) cont.resume(shown)
                }

                override fun onAdFailed(errorCode: Int, message: String?) {
                    android.util.Log.w(TAG, "百度插屏加载失败[$errorCode]: $message")
                    if (cont.isActive) cont.resume(false)
                }

                override fun onNoAd(errorCode: Int, message: String?) {
                    android.util.Log.w(TAG, "百度插屏无填充[$errorCode]: $message")
                    if (cont.isActive) cont.resume(false)
                }

                override fun onADExposed() {
                    shown = true
                }

                override fun onADExposureFailed() {
                    if (cont.isActive) cont.resume(false)
                }

                override fun onAdCacheSuccess() {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    try {
                        ad.show(activity)
                    } catch (t: Throwable) {
                        android.util.Log.e(TAG, "百度插屏展示失败", t)
                        if (cont.isActive) cont.resume(false)
                    }
                }

                override fun onAdCacheFailed() {
                    android.util.Log.w(TAG, "百度插屏缓存失败")
                    if (cont.isActive) cont.resume(false)
                }

                override fun onLpClosed() {}
            })

            ad.load()
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
            var adRef: RewardVideoAd? = null

            val listener = object : RewardVideoAd.RewardVideoAdListener {
                override fun onAdShow() {
                    shown = true
                }

                override fun onAdClick() {}

                override fun onAdClose(playScale: Float) {
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

                override fun onAdFailed(reason: String?) {
                    adRef = null
                    if (cont.isActive) {
                        cont.resume(RewardResult(
                            finished = true,
                            errorMessage = reason ?: "load_failed",
                        ))
                    }
                }

                override fun onVideoDownloadSuccess() {
                    val ad = adRef ?: return
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return

                    // ECPM 日志
                    try {
                        android.util.Log.i(TAG, "【ECPM-百度激励】ecpmLevel=${ad.ecpmLevel} pecpm=${ad.pecpm} posId=$codeId")
                    } catch (_: Throwable) {}

                    try {
                        ad.show(activity)
                    } catch (t: Throwable) {
                        adRef = null
                        if (cont.isActive) {
                            cont.resume(RewardResult(finished = true, errorMessage = "show_failed: ${t.message}"))
                        }
                    }
                }

                override fun onVideoDownloadFailed() {
                    // 由 onAdFailed 触发
                }

                override fun playCompletion() {}

                override fun onAdLoaded() {}

                override fun onAdSkip(playScale: Float) {
                    // 用户跳过，不发放奖励
                }

                override fun onRewardVerify(verify: Boolean, rewardExtra: Map<String, Any>?) {
                    rewardGranted = verify
                }
            }

            // true = 竜屏
            val ad = RewardVideoAd(activity, codeId, listener, true)
            adRef = ad
            val uid = tokenRepo.cachedUserId?.toString()
            if (!uid.isNullOrEmpty()) ad.setUserId(uid)
            ad.load()
        }
    }

    override suspend fun showSplashAd(
        activity: Activity, codeId: String, container: ViewGroup,
        onAdLoaded: (() -> Unit)?, onAdShown: (() -> Unit)?,
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return suspendCancellableCoroutine { cont ->
            val dm = activity.resources.displayMetrics
            val params = RequestParameters.Builder()
                .addExtra(SplashAd.KEY_TIMEOUT, SPLASH_TIMEOUT_MS.toString())
                .addExtra(SplashAd.KEY_DISPLAY_DOWNLOADINFO, "true")
                .addExtra(SplashAd.KEY_POPDIALOG_DOWNLOAD, "true")
                .setWidth(Math.max(1, (dm.widthPixels / dm.density).toInt()))
                .setHeight(Math.max(1, (dm.heightPixels / dm.density).toInt()))
                .build()

            var splashAd: SplashAd? = null

            cont.invokeOnCancellation {
                splashAd?.destroy()
                try { container.removeAllViews() } catch (_: Exception) {}
            }

            splashAd = SplashAd(activity, codeId, params, object : SplashInteractionListener {
                override fun onLpClosed() {
                    android.util.Log.i(TAG, "百度开屏 onLpClosed")
                    cleanupAndResume()
                }
                override fun onAdDismissed() {
                    android.util.Log.i(TAG, "百度开屏 onAdDismissed")
                    cleanupAndResume()
                }
                override fun onAdSkip() {
                    android.util.Log.i(TAG, "百度开屏 onAdSkip")
                    cleanupAndResume()
                }

                override fun onADLoaded() {
                    android.util.Log.i(TAG, "百度开屏 onADLoaded")
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return
                    onAdLoaded?.invoke()
                    container.post {
                        container.removeAllViews()
                        splashAd?.show(container)
                        onAdShown?.invoke()
                    }
                }

                override fun onAdPresent() {
                    android.util.Log.i(TAG, "百度开屏 onAdPresent")
                    onAdShown?.invoke()
                }
                override fun onAdExposed() {
                    android.util.Log.i(TAG, "百度开屏 onAdExposed")
                }
                override fun onAdClick() {}
                override fun onAdCacheSuccess() {
                    android.util.Log.i(TAG, "百度开屏 onAdCacheSuccess")
                }

                override fun onAdFailed(reason: String) {
                    android.util.Log.e(TAG, "百度开屏 onAdFailed: $reason")
                    if (cont.isActive) cont.resume(false)
                }
                override fun onAdCacheFailed() {
                    android.util.Log.e(TAG, "百度开屏 onAdCacheFailed")
                    if (cont.isActive) cont.resume(false)
                }

                private fun cleanupAndResume() {
                    try { container.removeAllViews() } catch (_: Exception) {}
                    splashAd?.destroy()
                    if (cont.isActive) cont.resume(true)
                }
            })
            android.util.Log.i(TAG, "百度开屏 load() 调用")
            splashAd.load()
        }
    }

    companion object {
        private const val TAG = "BaiduAdProvider"
        private const val SPLASH_TIMEOUT_MS = 5000
    }
}
