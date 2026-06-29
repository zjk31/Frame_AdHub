package com.example.adhub.data.provider.gdt

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
class GdtAdProvider : AdProvider {

    private val _bannerState = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
    private val _feedState = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)

    override suspend fun initialize(context: Context): Result<Unit> {
        // TODO: GdtAdSdkManager.init(context)
        return Result.success(Unit)
    }

    override fun isReady(): Boolean {
        // TODO: GdtAdSdkManager 状态
        return false
    }

    override fun revokePrivacyConsent(context: Context) {
        // TODO
    }

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        // TODO: GdtUnifiedBannerPlatformView
        return _bannerState
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        // TODO: GdtNativeExpressFeedPlatformView
        return _feedState
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        // TODO: GdtUnifiedInterstitialPlatformView
        return false
    }

    override suspend fun showRewardVideo(
        activity: Activity,
        codeId: String,
        slotKey: String,
    ): RewardResult {
        // TODO: GdtRewardVideoPlatformView（按 slotKey 选择子类）
        return RewardResult(shown = false, finished = false, rewardGranted = false)
    }

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean {
        // TODO: 优量汇开屏
        return false
    }
}
