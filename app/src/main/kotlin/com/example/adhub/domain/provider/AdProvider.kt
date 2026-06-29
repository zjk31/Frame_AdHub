package com.example.adhub.domain.provider

import android.app.Activity
import android.content.Context
import android.view.View
import com.example.adhub.domain.model.AdLoadState
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.model.RewardResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 广告策略接口 —— 每个通道（友盟/穿山甲/优量汇/百度）独立实现。
 *
 * Domain 层只依赖此接口，UI 层和 Data 层通过它解耦。
 * 新增通道只需新增一个实现类。
 */
interface AdProvider {

    // ── 生命周期 ──

    /** 初始化 SDK（可能异步回调，建议 suspend 包装）。 */
    suspend fun initialize(context: Context): Result<Unit>

    /** SDK 是否就绪。 */
    fun isReady(): Boolean

    /** 撤销隐私同意。 */
    fun revokePrivacyConsent(context: Context)

    // ── 广告加载 ──

    /** 加载 Banner（返回包含 [View] 的 [StateFlow]，Compose 通过 [androidx.compose.ui.viewinterop.AndroidView] 包裹）。 */
    fun loadBanner(codeId: String): StateFlow<AdLoadState<View>>

    /** 加载信息流。 */
    fun loadFeed(codeId: String, count: Int): StateFlow<AdLoadState<List<View>>>

    // ── 全屏广告 ──

    /** 展示插屏（挂起直到广告关闭）。 */
    suspend fun showInterstitial(activity: Activity, codeId: String): Boolean

    /** 展示激励视频（挂起直到广告播放完，返回完整回调信息）。 */
    suspend fun showRewardVideo(
        activity: Activity,
        codeId: String,
        slotKey: String = AdPlacement.Reward.slotKey,
    ): RewardResult

    // ── 开屏 ──

    /** 展示开屏广告（挂起直到开屏关闭）。 */
    suspend fun showSplashAd(activity: Activity, codeId: String): Boolean
}
