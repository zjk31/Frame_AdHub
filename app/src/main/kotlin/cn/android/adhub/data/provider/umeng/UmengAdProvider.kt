package cn.android.adhub.data.provider.umeng

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import cn.android.adhub.R
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.domain.model.AdLoadState
import cn.android.adhub.domain.model.RewardResult
import cn.android.adhub.domain.provider.AdProvider
import com.umeng.commonsdk.UMConfigure
import com.umeng.union.UMRewardAD
import com.umeng.union.UMSplashAD
import com.umeng.union.UMUnionSdk
import com.umeng.union.api.UMAdConfig
import com.umeng.union.api.UMUnionApi
import com.umeng.union.widget.UMNativeLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.coroutines.resume

class UmengAdProvider : AdProvider {

    @Volatile private var initialized = false

    override suspend fun initialize(context: Context): Result<Unit> {
        if (initialized) return Result.success(Unit)
        return try {
            withContext(Dispatchers.Main) {
                val app = context.applicationContext
                UMConfigure.submitPolicyGrantResult(app, true)
                UMConfigure.init(app, UmengConfig.APP_KEY, UmengConfig.CHANNEL,
                    UMConfigure.DEVICE_TYPE_PHONE, "")
                UMUnionSdk.init(app)
            }
            initialized = true
            android.util.Log.i(TAG, "Umeng SDK init done, appKey=${UmengConfig.APP_KEY}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Umeng SDK init failed", e)
            Result.failure(e)
        }
    }

    override fun isReady(): Boolean = initialized

    override fun revokePrivacyConsent(context: Context) {
        UMConfigure.submitPolicyGrantResult(context.applicationContext, false)
    }

    // ── Banner（对齐 flutter_merge: XML 布局 + 下载图片 + bindView） ──

    override fun loadBanner(codeId: String, activity: Activity?): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val config = UMAdConfig.Builder().setSlotId(codeId).build()
        UMUnionSdk.loadNativeBannerAd(config, object : UMUnionApi.AdLoadListener<com.umeng.union.UMNativeAD> {
            override fun onSuccess(type: UMUnionApi.AdType?, ad: com.umeng.union.UMNativeAD) {
                val root = LayoutInflater.from(ctx).inflate(R.layout.umeng_native_banner, null) as UMNativeLayout
                val imageView = root.findViewById<ImageView>(R.id.umeng_native_image)

                // 下载广告图片
                val imgUrl = ad.imageUrl
                if (!imgUrl.isNullOrBlank()) {
                    Thread {
                        try {
                            val bmp = BitmapFactory.decodeStream(URL(imgUrl).openStream())
                            imageView.post { imageView.setImageBitmap(bmp) }
                        } catch (_: Exception) {}
                    }.start()
                }

                ad.setAdEventListener(object : UMUnionApi.AdEventListener {
                    override fun onExposed() {}
                    override fun onClicked(v: View?) {}
                    override fun onError(code: Int, msg: String?) {
                        android.util.Log.e(TAG, "Banner error: code=$code msg=$msg")
                    }
                })
                ad.bindView(ctx, root, listOf(root, imageView))
                state.value = AdLoadState.Loaded(root)
            }

            override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                state.value = AdLoadState.Error("Umeng Banner: $message")
            }
        })
        return state
    }

    // ── 信息流（对齐 flutter_merge: XML 布局 + 图片/图标 + 标题/描述 + bindView） ──

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        val state = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val config = UMAdConfig.Builder().setSlotId(codeId).build()
        UMUnionSdk.loadFeedAd(config, object : UMUnionApi.AdLoadListener<com.umeng.union.UMNativeAD> {
            override fun onSuccess(type: UMUnionApi.AdType?, ad: com.umeng.union.UMNativeAD) {
                val root = LayoutInflater.from(ctx).inflate(R.layout.umeng_native_feed, null) as UMNativeLayout
                val imageView = root.findViewById<ImageView>(R.id.umeng_feed_image)
                val iconView = root.findViewById<ImageView>(R.id.umeng_feed_icon)
                val titleView = root.findViewById<TextView>(R.id.umeng_feed_title)
                val descView = root.findViewById<TextView>(R.id.umeng_feed_desc)

                titleView.text = ad.title ?: ""
                descView.text = ad.content ?: ""

                // 下载素材图
                val imgUrl = ad.imageUrl
                if (!imgUrl.isNullOrBlank()) {
                    Thread {
                        try {
                            val bmp = BitmapFactory.decodeStream(URL(imgUrl).openStream())
                            imageView.post { imageView.setImageBitmap(bmp) }
                        } catch (_: Exception) {}
                    }.start()
                }
                // 下载图标
                val iconUrl = ad.iconUrl
                if (!iconUrl.isNullOrBlank()) {
                    Thread {
                        try {
                            val bmp = BitmapFactory.decodeStream(URL(iconUrl).openStream())
                            iconView.post { iconView.setImageBitmap(bmp) }
                        } catch (_: Exception) {}
                    }.start()
                }

                ad.setAdEventListener(object : UMUnionApi.AdEventListener {
                    override fun onExposed() {}
                    override fun onClicked(v: View?) {}
                    override fun onError(code: Int, msg: String?) {
                        android.util.Log.e(TAG, "Feed error: code=$code msg=$msg")
                    }
                })
                ad.bindView(ctx, root, listOf(root, imageView, iconView))
                state.value = AdLoadState.Loaded(listOf(root))
            }

            override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                state.value = AdLoadState.Error("Umeng Feed: $message")
            }
        })
        return state
    }

    // ── 插屏（对齐 flutter_merge: runOnUiThread + show(activity)） ──

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (codeId.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            val config = UMAdConfig.Builder().setSlotId(codeId).build()

            UMUnionSdk.getApi().loadInterstitialAd(activity, config,
                object : UMUnionApi.AdLoadListener<UMUnionApi.AdDisplay> {
                    override fun onSuccess(type: UMUnionApi.AdType?, display: UMUnionApi.AdDisplay) {
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed || !cont.isActive) {
                                try { display.destroy() } catch (_: Throwable) {}
                                return@runOnUiThread
                            }

                            var shown = false

                            display.setAdCloseListener { _ ->
                                if (cont.isActive) cont.resume(shown)
                            }

                            display.setAdEventListener(object : UMUnionApi.AdEventListener {
                                override fun onExposed() { shown = true }
                                override fun onClicked(view: View?) {}
                                override fun onError(code: Int, message: String?) {
                                    if (cont.isActive) cont.resume(false)
                                }
                            })

                            try {
                                display.show(activity)
                                shown = true
                            } catch (t: Throwable) {
                                try { display.destroy() } catch (_: Throwable) {}
                                android.util.Log.e(TAG, "插屏展示失败", t)
                                if (cont.isActive) cont.resume(false)
                            }
                        }
                    }

                    override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                        android.util.Log.w(TAG, "插屏加载失败: $message")
                        if (cont.isActive) cont.resume(false)
                    }
                })
        }
    }

    // ── 激励视频（对齐 flutter_merge: runOnUiThread + show(activity)） ──

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
                    activity.runOnUiThread {
                        if (activity.isFinishing || activity.isDestroyed || !cont.isActive) {
                            try { ad.destroy() } catch (_: Throwable) {}
                            return@runOnUiThread
                        }

                        var shown = false
                        var rewardGranted = false

                        ad.setAdEventListener(object : UMUnionApi.RewardAdListener {
                            override fun onExposed() { shown = true }
                            override fun onClicked(view: View?) {}
                            override fun onError(code: Int, message: String?) {
                                if (cont.isActive) {
                                    cont.resume(RewardResult(
                                        shown = shown, finished = true,
                                        errorMessage = "onError[$code]: ${message ?: ""}",
                                    ))
                                }
                            }
                            override fun onDismissed() {
                                if (cont.isActive) {
                                    cont.resume(RewardResult(
                                        shown = shown, finished = true,
                                        rewardGranted = rewardGranted, rewardType = slotKey,
                                    ))
                                }
                            }
                            override fun onReward(valid: Boolean, extra: Map<String, Any>?) {
                                rewardGranted = valid
                            }
                        })

                        try {
                            ad.show(activity)
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

    // ── 开屏 ──

    override suspend fun showSplashAd(
        activity: Activity, codeId: String, container: ViewGroup,
        onAdLoaded: (() -> Unit)?, onAdShown: (() -> Unit)?,
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                try { container.removeAllViews() } catch (_: Exception) {}
            }

            val config = UMAdConfig.Builder().setSlotId(codeId).build()

            UMUnionSdk.loadSplashAd(config, object : UMUnionApi.AdLoadListener<UMSplashAD> {
                override fun onSuccess(type: UMUnionApi.AdType?, ad: UMSplashAD) {
                    if (activity.isFinishing || activity.isDestroyed || !cont.isActive) return
                    onAdLoaded?.invoke()
                    ad.setAdEventListener(object : UMUnionApi.SplashAdListener {
                        override fun onExposed() { onAdShown?.invoke() }
                        override fun onClicked(view: View?) {}
                        override fun onError(code: Int, message: String?) {
                            android.util.Log.e(TAG, "友盟开屏错误: code=$code msg=$message")
                        }
                        override fun onDismissed() {
                            try { container.removeAllViews() } catch (_: Exception) {}
                            if (cont.isActive) cont.resume(true)
                        }
                    })
                    container.post {
                        container.removeAllViews()
                        ad.show(container)
                        onAdShown?.invoke()
                    }
                }

                override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                    android.util.Log.e(TAG, "友盟开屏加载失败: $message")
                    if (cont.isActive) cont.resume(false)
                }
            }, SPLASH_TIMEOUT_MS)
        }
    }

    companion object {
        private const val TAG = "UmengAdProvider"
        private const val SPLASH_TIMEOUT_MS = 5000
    }
}
