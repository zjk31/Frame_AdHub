package com.example.adhub.data.provider.umeng

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import com.umeng.union.UMNativeAD
import com.umeng.union.UMSplashAD
import com.umeng.union.UMUnionSdk
import com.umeng.union.api.UMAdConfig
import com.umeng.union.api.UMUnionApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Single
import java.net.URL
import kotlin.coroutines.resume

@Single(binds = [AdProvider::class])
class UmengAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isReady(): Boolean = true
    override fun revokePrivacyConsent(context: Context) {}

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context

        val config = UMAdConfig.Builder().setSlotId(codeId).build()
        UMUnionSdk.loadNativeBannerAd(config, object : UMUnionApi.AdLoadListener<UMNativeAD> {
            override fun onSuccess(type: UMUnionApi.AdType?, ad: UMNativeAD) {
                ad.setAdEventListener(object : UMUnionApi.AdEventListener {
                    override fun onExposed() {}
                    override fun onClicked(v: View?) {}
                    override fun onError(code: Int, msg: String?) {}
                })
                val container = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                val imageView = ImageView(ctx)
                container.addView(imageView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (ctx.resources.displayMetrics.widthPixels * 9f / 16f).toInt()
                ))
                ad.bindView(ctx, container, listOf(container, imageView))

                Thread {
                    try {
                        val bitmap = BitmapFactory.decodeStream(URL(ad.imageUrl).openStream())
                        imageView.post { imageView.setImageBitmap(bitmap) }
                    } catch (_: Exception) {}
                }.start()

                state.value = AdLoadState.Loaded(container)
            }

            override fun onFailure(type: UMUnionApi.AdType?, message: String?) {
                state.value = AdLoadState.Error("Umeng Banner: $message")
            }
        })
        return state
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        return MutableStateFlow(AdLoadState.Error("Not implemented"))
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean = false
    override suspend fun showRewardVideo(
        activity: Activity, codeId: String, slotKey: String,
    ): RewardResult = RewardResult(shown = false, finished = false, rewardGranted = false)

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
