package com.example.adhub.data.provider.csj

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single(binds = [AdProvider::class])
class CsjAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        // TODO: CsjAdSdkManager.init(context) + TTAdSdk 回调
        return Result.success(Unit)
    }

    override fun isReady(): Boolean {
        return try {
            TTAdSdk.isInitSuccess()
        } catch (_: Exception) { false }
    }

    override fun revokePrivacyConsent(context: Context) {
        // TODO: 穿山甲隐私合规撤销
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
        return MutableStateFlow(AdLoadState.Error("Not implemented"))
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean = false
    override suspend fun showRewardVideo(
        activity: Activity, codeId: String, slotKey: String,
    ): RewardResult = RewardResult(shown = false, finished = false, rewardGranted = false)

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean = false

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
}
