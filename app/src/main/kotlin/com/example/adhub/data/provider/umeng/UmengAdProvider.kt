package com.example.adhub.data.provider.umeng

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.umeng.union.widget.UMNativeLayout
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import com.umeng.union.UMRewardAD
import com.umeng.union.UMSplashAD
import com.umeng.union.UMUnionSdk
import com.umeng.union.api.UMAdConfig
import com.umeng.union.api.UMUnionApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URL
import kotlin.coroutines.resume

class UmengAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        return try {
            // 友盟 SDK 初始化——需在 Application.onCreate 中调用
            UMUnionSdk.init(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = true

    override fun revokePrivacyConsent(context: Context) {}

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val config = UMAdConfig.Builder().setSlotId(codeId).build()
        UMUnionSdk.loadNativeBannerAd(config, object : UMUnionApi.AdLoadListener<com.umeng.union.UMNativeAD> {
            override fun onSuccess(type: UMUnionApi.AdType?, ad: com.umeng.union.UMNativeAD) {
                ad.setAdEventListener(object : UMUnionApi.AdEventListener {
                    override fun onExposed() {}
                    override fun onClicked(v: View?) {}
                    override fun onError(code: Int, msg: String?) {}
                })
                // 使用 SDK 的 UMNativeLayout 作为容器
                val nativeLayout = UMNativeLayout(ctx)
                ad.bindView(ctx, nativeLayout, listOf(nativeLayout))
                state.value = AdLoadState.Loaded(nativeLayout)
            }

            override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                state.value = AdLoadState.Error("Umeng Banner: $message")
            }
        })
        return state
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val config = UMAdConfig.Builder().setSlotId(codeId).build()
        UMUnionSdk.loadFeedAd(config, object : UMUnionApi.AdLoadListener<com.umeng.union.UMNativeAD> {
            override fun onSuccess(type: UMUnionApi.AdType?, ad: com.umeng.union.UMNativeAD) {
                ad.setAdEventListener(object : UMUnionApi.AdEventListener {
                    override fun onExposed() {}
                    override fun onClicked(v: View?) {}
                    override fun onError(code: Int, msg: String?) {}
                })

                val nativeLayout = UMNativeLayout(ctx)
                ad.bindView(ctx, nativeLayout, listOf(nativeLayout))
                state.value = AdLoadState.Loaded(listOf(nativeLayout))
            }

            override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                state.value = AdLoadState.Error("Umeng Feed: $message")
            }
        })
        return state
    }

    // ── 插屏 ──
    // 参考 flutter_merge UmPangleInterstitialPlatformView:
    // UMUnionSdk.getApi().loadInterstitialAd(activity, config, listener) → onSuccess → display.show(activity)
    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (codeId.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            val config = UMAdConfig.Builder().setSlotId(codeId).build()

            UMUnionSdk.getApi().loadInterstitialAd(activity, config,
                object : UMUnionApi.AdLoadListener<UMUnionApi.AdDisplay> {
                    override fun onSuccess(type: UMUnionApi.AdType?, display: UMUnionApi.AdDisplay) {
                        if (activity.isFinishing || activity.isDestroyed || !cont.isActive) {
                            try { display.destroy() } catch (_: Throwable) {}
                            return
                        }

                        var shown = false

                        display.setAdCloseListener { _ ->
                            // 插屏关闭
                            if (cont.isActive) cont.resume(shown)
                        }

                        display.setAdEventListener(object : UMUnionApi.AdEventListener {
                            override fun onExposed() {
                                shown = true
                            }

                            override fun onClicked(view: View?) {}

                            override fun onError(code: Int, message: String?) {
                                if (cont.isActive) {
                                    cont.resume(false)
                                }
                            }
                        })

                        try {
                            display.show(activity)
                            shown = true
                        } catch (t: Throwable) {
                            try { display.destroy() } catch (_: Throwable) {}
                            android.util.Log.e("UmengAdProvider", "插屏展示失败", t)
                            if (cont.isActive) cont.resume(false)
                        }
                    }

                    override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                        android.util.Log.w("UmengAdProvider", "插屏加载失败: $message")
                        if (cont.isActive) cont.resume(false)
                    }
                })
        }
    }

    // ── 激励视频 ──
    // 参考 flutter_merge UmPangleRewardPlatformView:
    // UMUnionSdk.getApi().loadRewardAd(config, listener) → onSuccess → display.show(activity)
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
            val config = UMAdConfig.Builder().setSlotId(codeId).build()

            UMUnionSdk.getApi().loadRewardAd(config, object : UMUnionApi.AdLoadListener<UMRewardAD> {
                override fun onSuccess(type: UMUnionApi.AdType?, ad: UMRewardAD) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) {
                        try { ad.destroy() } catch (_: Throwable) {}
                        return
                    }

                    var shown = false
                    var rewardGranted = false

                    ad.setAdEventListener(object : UMUnionApi.RewardAdListener {
                        override fun onExposed() {
                            shown = true
                        }

                        override fun onClicked(view: View?) {}

                        override fun onError(code: Int, message: String?) {
                            // 展示期间出错
                            if (cont.isActive) {
                                cont.resume(RewardResult(
                                    shown = shown,
                                    finished = true,
                                    errorMessage = "onError[$code]: ${message ?: ""}",
                                ))
                            }
                        }

                        override fun onDismissed() {
                            if (cont.isActive) {
                                cont.resume(RewardResult(
                                    shown = shown,
                                    finished = true,
                                    rewardGranted = rewardGranted,
                                    rewardType = slotKey,
                                ))
                            }
                        }

                        override fun onReward(valid: Boolean, extra: Map<String, Any>?) {
                            rewardGranted = valid
                        }
                    })

                    try {
                        ad.show()
                    } catch (t: Throwable) {
                        try { ad.destroy() } catch (_: Throwable) {}
                        if (cont.isActive) {
                            cont.resume(RewardResult(
                                finished = true,
                                errorMessage = "show_failed: ${t.message}",
                            ))
                        }
                    }
                }

                override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                    if (cont.isActive) {
                        cont.resume(RewardResult(
                            finished = true,
                            errorMessage = "load_failed: ${message ?: ""}",
                        ))
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

            cont.invokeOnCancellation {
                try { decorView.removeView(container) } catch (_: Exception) {}
            }

            val config = UMAdConfig.Builder().setSlotId(codeId).build()

            UMUnionSdk.loadSplashAd(config, object : UMUnionApi.AdLoadListener<UMSplashAD> {
                override fun onSuccess(type: UMUnionApi.AdType?, ad: UMSplashAD) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return
                    ad.setAdEventListener(object : UMUnionApi.SplashAdListener {
                        override fun onDismissed() {
                            try { decorView.removeView(container) } catch (_: Exception) {}
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onExposed() {}
                        override fun onClicked(view: View?) {}
                        override fun onError(code: Int, message: String?) {}
                    })
                    decorView.post {
                        decorView.addView(container)
                        ad.show(container)
                    }
                }

                override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                    if (cont.isActive) cont.resume(false)
                }
            }, SPLASH_TIMEOUT_MS)
        }
    }

    companion object {
        private const val SPLASH_TIMEOUT_MS = 5000
    }
}
