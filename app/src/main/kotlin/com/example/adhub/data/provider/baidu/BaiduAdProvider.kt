package com.example.adhub.data.provider.baidu

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.baidu.mobads.sdk.api.BaiduNativeManager
import com.baidu.mobads.sdk.api.ExpressResponse
import com.baidu.mobads.sdk.api.RequestParameters
import com.example.adhub.core.AppContextHolder
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single(binds = [AdProvider::class])
class BaiduAdProvider : AdProvider {

    override suspend fun initialize(context: Context): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isReady(): Boolean = true
    override fun revokePrivacyConsent(context: Context) {}

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        val state = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
        val ctx = AppContextHolder.context
        val activity = ctx as? Activity ?: run {
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
            RequestParameters.Builder().build(),
            object : BaiduNativeManager.ExpressAdListener {
                override fun onNativeLoad(responses: MutableList<ExpressResponse>?) {
                    if (responses.isNullOrEmpty()) {
                        state.value = AdLoadState.Error("Baidu: 无广告返回")
                        loading.visibility = View.GONE
                        return
                    }
                    val ad = responses[0]
                    ad.bindInteractionActivity(activity)
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
        return MutableStateFlow(AdLoadState.Error("Not implemented"))
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean = false
    override suspend fun showRewardVideo(
        activity: Activity, codeId: String, slotKey: String,
    ): RewardResult = RewardResult(shown = false, finished = false, rewardGranted = false)

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean = false
}
