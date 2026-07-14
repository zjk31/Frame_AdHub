package cn.maga.lingdongmanhua.domain.provider

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import cn.maga.lingdongmanhua.domain.model.AdLoadState
import cn.maga.lingdongmanhua.domain.model.AdPlacement
import cn.maga.lingdongmanhua.domain.model.RewardResult
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

    /** 加载 Banner（返回包含 [View] 的 [StateFlow]，Compose 通过 [androidx.compose.ui.viewinterop.AndroidView] 包裹）。
     *
     * @param activity 部分 SDK（GDT/百度）的 Banner 构造需要 Activity Context，
     *                 传 null 时这些 SDK 会尝试用 Application context 并可能失败。 */
    fun loadBanner(codeId: String, activity: Activity? = null): StateFlow<AdLoadState<View>>

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

    /** 展示开屏广告（挂起直到开屏关闭）。
     *
     * 对齐 flutter_merge 预加载模式：
     * - [onAdLoaded]：广告素材加载完成（取消启动超时）
     * - [onAdShown]：广告真正可见（释放系统 SplashScreen，避免 splash 动画干扰 CSJ 倒计时）
     *
     * @param container 广告渲染容器（XML 定义的 splash_ad_container）
     * @param onAdLoaded 广告素材加载完成时回调，默认 null
     * @param onAdShown 广告首次可见时回调，默认 null */
    suspend fun showSplashAd(
        activity: Activity,
        codeId: String,
        container: ViewGroup,
        onAdLoaded: (() -> Unit)? = null,
        onAdShown: (() -> Unit)? = null,
    ): Boolean
}
