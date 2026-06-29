package com.example.adhub.data.provider.csj

import android.app.Activity
import android.content.Context
import android.view.View
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.model.RewardResult
import com.example.adhub.domain.provider.AdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single(binds = [AdProvider::class])
class CsjAdProvider : AdProvider {

    private val _bannerState = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
    private val _feedState = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)

    override suspend fun initialize(context: Context): Result<Unit> {
        // TODO: CsjAdSdkManager.init(context) + TTAdSdk 回调
        return Result.success(Unit)
    }

    override fun isReady(): Boolean {
        // TODO: TTAdSdk.isInitSuccess()
        return false
    }

    override fun revokePrivacyConsent(context: Context) {
        // TODO: 穿山甲隐私合规撤销
    }

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        // TODO: CsjPangleBannerPlatformViewFactory
        return _bannerState
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        // TODO: CsjPangleFreePlatformView
        return _feedState
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        // TODO: Intent → CsjInterstitialFullActivity
        return false
    }

    override suspend fun showRewardVideo(
        activity: Activity,
        codeId: String,
        slotKey: String,
    ): RewardResult {
        // TODO: Intent → CsjReadRewardVideoActivity / CsjTaskRewardVideoActivity 等
        return RewardResult(shown = false, finished = false, rewardGranted = false)
    }

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean {
        // TODO: 穿山甲开屏
        return false
    }
}
