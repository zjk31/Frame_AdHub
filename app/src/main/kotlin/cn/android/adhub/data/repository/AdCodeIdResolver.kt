package cn.android.adhub.data.repository

import cn.android.adhub.data.provider.baidu.BaiduConfig
import cn.android.adhub.data.provider.csj.CsjConfig
import cn.android.adhub.data.provider.gdt.GdtConfig
import cn.android.adhub.data.provider.umeng.UmengConfig
import cn.android.adhub.domain.model.AdChannel
import cn.android.adhub.domain.model.AdPlacement

/**
 * 根据当前通道 + 广告位类型，解析对应的代码位 ID。
 *
 * 参考 flutter_merge 的 [AppAdConfig] 逻辑，用 Kotlin [when] 替代原有 if-else 链。
 */
object AdCodeIdResolver {

    fun resolve(channel: AdChannel, placement: AdPlacement): String = when (channel) {
        AdChannel.Umeng -> resolveUmeng(placement)
        AdChannel.Csj   -> resolveCsj(placement)
        AdChannel.Gdt   -> resolveGdt(placement)
        AdChannel.Baidu -> resolveBaidu(placement)
    }

    private fun resolveUmeng(p: AdPlacement) = when (p) {
        AdPlacement.Splash      -> UmengConfig.CODE_ID_SPLASH
        AdPlacement.Banner      -> UmengConfig.BANNER_CODE_ID
        AdPlacement.BannerMini  -> UmengConfig.BANNER_MINI_CODE_ID
        AdPlacement.Interstitial -> UmengConfig.INTERSTITIAL_CODE_ID
        AdPlacement.Feed        -> UmengConfig.FEED_CODE_ID
        AdPlacement.Reward,
        AdPlacement.TaskReward,
        AdPlacement.PureReward,
        AdPlacement.DownloadQuotaReward -> UmengConfig.REWARD_CODE_ID
    }

    private fun resolveCsj(p: AdPlacement) = when (p) {
        AdPlacement.Splash      -> CsjConfig.SPLASH_CODE_ID
        AdPlacement.Banner      -> CsjConfig.BANNER_CODE_ID
        AdPlacement.BannerMini  -> CsjConfig.BANNER_MINI_CODE_ID
        AdPlacement.Interstitial -> CsjConfig.AD_SLOT_INTERSTITIAL
        AdPlacement.Feed        -> CsjConfig.AD_FREE_CODE_ID
        AdPlacement.Reward      -> CsjConfig.AD_SLOT_REWARD_VIDEO
        AdPlacement.TaskReward  -> CsjConfig.AD_SLOT_TASK_REWARD_VIDEO
        AdPlacement.PureReward  -> CsjConfig.AD_SLOT_PURE_REWARD_VIDEO
        AdPlacement.DownloadQuotaReward -> CsjConfig.AD_SLOT_DOWNLOAD_QUOTA_REWARD_VIDEO
    }

    private fun resolveGdt(p: AdPlacement) = when (p) {
        AdPlacement.Splash      -> GdtConfig.SPLASH_POS_ID
        AdPlacement.Banner      -> GdtConfig.BANNER_POS_ID
        AdPlacement.BannerMini  -> GdtConfig.BANNER_MINI_POS_ID
        AdPlacement.Interstitial -> GdtConfig.INTERSTITIAL_POS_ID
        AdPlacement.Feed        -> GdtConfig.FEED_POS_ID
        AdPlacement.Reward      -> GdtConfig.REWARD_POS_ID
        AdPlacement.TaskReward  -> GdtConfig.TASK_REWARD_POS_ID
        AdPlacement.PureReward  -> GdtConfig.PURE_REWARD_POS_ID
        AdPlacement.DownloadQuotaReward -> GdtConfig.DOWNLOAD_QUOTA_REWARD_POS_ID
    }

    private fun resolveBaidu(p: AdPlacement) = when (p) {
        AdPlacement.Splash      -> BaiduConfig.SPLASH_PLACE_ID
        AdPlacement.Banner      -> BaiduConfig.BANNER_PLACE_ID
        AdPlacement.BannerMini  -> BaiduConfig.BANNER_MINI_PLACE_ID
        AdPlacement.Interstitial -> BaiduConfig.INTERSTITIAL_PLACE_ID
        AdPlacement.Feed        -> BaiduConfig.FEED_PLACE_ID
        AdPlacement.Reward,
        AdPlacement.TaskReward,
        AdPlacement.PureReward,
        AdPlacement.DownloadQuotaReward -> BaiduConfig.REWARD_PLACE_ID
    }
}
