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
import com.qq.e.ads.splash.SplashAD
import com.qq.e.ads.splash.SplashADListener
import com.qq.e.comm.util.AdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.annotation.Single
import kotlin.coroutines.resume

@Single(binds = [AdProvider::class])
class GdtAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isReady(): Boolean = true
    override fun revokePrivacyConsent(context: Context) {}

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val activity = ctx as? Activity ?: run {
            state.value = AdLoadState.Error("GDT: Context is not Activity")
            return state
        }

        val bannerView = UnifiedBannerView(activity, codeId, object : UnifiedBannerADListener {
            override fun onADReceive() {
                // Banner 加载成功，UnifiedBannerView 本身就是 View
            }

            override fun onNoAD(error: AdError) {
                state.value = AdLoadState.Error("GDT no ad: ${error.errorMsg}")
            }

            override fun onADExposure() {}
            override fun onADClosed() {}
            override fun onADClicked() {}
            override fun onADLeftApplication() {}
        })

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
        bannerView.loadAD()
        state.value = AdLoadState.Loaded(container)
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
    }
}
