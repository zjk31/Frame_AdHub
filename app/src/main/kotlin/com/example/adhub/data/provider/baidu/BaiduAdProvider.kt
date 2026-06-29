package com.example.adhub.data.provider.baidu

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
class BaiduAdProvider : AdProvider {

    private val _bannerState = MutableStateFlow<AdLoadState<View>>(AdLoadState.Loading)
    private val _feedState = MutableStateFlow<AdLoadState<List<View>>>(AdLoadState.Loading)

    override suspend fun initialize(context: Context): Result<Unit> {
        // TODO: BaiduAdSdkManager.init(context)
        return Result.success(Unit)
    }

    override fun isReady(): Boolean {
        // TODO: BaiduAdSdkManager 状态
        return false
    }

    override fun revokePrivacyConsent(context: Context) {
        // TODO
    }

    override fun loadBanner(codeId: String): StateFlow<AdLoadState<View>> {
        // TODO: BaiduExpressFeedPlatformView（填充 Banner 区域）
        return _bannerState
    }

    override fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>> {
        // TODO: BaiduExpressFeedPlatformView
        return _feedState
    }

    override suspend fun showInterstitial(activity: Activity, codeId: String): Boolean {
        // TODO: BaiduInterstitialController.showInterstitial()
        return false
    }

    override suspend fun showRewardVideo(
        activity: Activity,
        codeId: String,
        slotKey: String,
    ): RewardResult {
        // TODO: BaiduRewardVideoController.showRewardVideo()
        return RewardResult(shown = false, finished = false, rewardGranted = false)
    }

    override suspend fun showSplashAd(activity: Activity, codeId: String): Boolean {
        // TODO: 百度开屏
        return false
    }
}
