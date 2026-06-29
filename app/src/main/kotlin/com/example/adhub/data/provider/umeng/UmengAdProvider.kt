package com.example.adhub.data.provider.umeng

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
class UmengAdProvider : AdProvider {

    private val _bannerState = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
    private val _feedState = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)

    override suspend fun initialize(context: Context): Result<Unit> {
        // TODO: 调用 UmAdSdkManager.ensureReady()
        return Result.success(Unit)
    }

    override fun isReady(): Boolean {
        // TODO: UmAdSdkManager.isInitialized()
        return false
    }

    override fun revokePrivacyConsent(context: Context) {
        // TODO: UmAdSdkManager.revokePrivacyConsent()
    }

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        return _bannerState
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        return _feedState
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        // TODO: 创建 UmPangleInterstitialPlatformView 并展示
        return false
    }

    override suspend fun showRewardVideo(
        activity: Activity,
        codeId: String,
        slotKey: String,
    ): RewardResult {
        // TODO: 创建 UmPangleRewardPlatformView 并监听回调
        return RewardResult(shown = false, finished = false, rewardGranted = false)
    }

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean {
        // TODO: 友盟开屏广告展示
        return false
    }
}
