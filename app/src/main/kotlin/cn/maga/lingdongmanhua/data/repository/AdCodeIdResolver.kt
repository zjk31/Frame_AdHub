package cn.maga.lingdongmanhua.data.repository

import cn.maga.lingdongmanhua.data.provider.baidu.BaiduConfig
import cn.maga.lingdongmanhua.data.provider.csj.CsjConfig
import cn.maga.lingdongmanhua.data.provider.gdt.GdtConfig
import cn.maga.lingdongmanhua.data.provider.umeng.UmengConfig
import cn.maga.lingdongmanhua.data.remote.RemoteConfigData
import cn.maga.lingdongmanhua.domain.model.AdChannel
import cn.maga.lingdongmanhua.domain.model.AdPlacement

/**
 * 根据当前通道 + 广告位类型，解析对应的代码位 ID。
 *
 * 对齐 flutter_merge：优先使用远程下发的代码位（[remoteConfig]），
 * 不可用时回退各 Provider Config 的硬编码值。
 */
object AdCodeIdResolver {

    fun resolve(
        channel: AdChannel,
        placement: AdPlacement,
        remoteConfig: RemoteConfigData? = null,
    ): String = when (channel) {
        AdChannel.Umeng -> resolveUmeng(placement, remoteConfig)
        AdChannel.Csj   -> resolveCsj(placement, remoteConfig)
        AdChannel.Gdt   -> resolveGdt(placement, remoteConfig)
        AdChannel.Baidu -> resolveBaidu(placement, remoteConfig)
    }

    private fun resolveCsj(p: AdPlacement, remote: RemoteConfigData?) = when (p) {
        AdPlacement.Splash      -> remote?.adSplashCode ?: CsjConfig.SPLASH_CODE_ID
        AdPlacement.Banner      -> remote?.adBannerCode ?: CsjConfig.BANNER_CODE_ID
        AdPlacement.BannerMini  -> remote?.adMiniBannerCode ?: CsjConfig.BANNER_MINI_CODE_ID
        AdPlacement.Interstitial -> remote?.adInterstitialCode ?: CsjConfig.AD_SLOT_INTERSTITIAL
        AdPlacement.Feed        -> remote?.adFreeCode ?: CsjConfig.AD_FREE_CODE_ID
        AdPlacement.Reward      -> remote?.adRewardCode ?: CsjConfig.AD_SLOT_REWARD_VIDEO
        AdPlacement.TaskReward  -> remote?.adTaskCode ?: CsjConfig.AD_SLOT_TASK_REWARD_VIDEO
        AdPlacement.PureReward  -> remote?.adPureCode ?: CsjConfig.AD_SLOT_PURE_REWARD_VIDEO
        AdPlacement.DownloadQuotaReward -> remote?.adDownloadCode ?: CsjConfig.AD_SLOT_DOWNLOAD_QUOTA_REWARD_VIDEO
    }

    private fun resolveUmeng(p: AdPlacement, remote: RemoteConfigData?) = when (p) {
        AdPlacement.Splash      -> remote?.adSplashCode ?: UmengConfig.CODE_ID_SPLASH
        AdPlacement.Banner      -> remote?.adBannerCode ?: UmengConfig.BANNER_CODE_ID
        AdPlacement.BannerMini  -> remote?.adMiniBannerCode ?: UmengConfig.BANNER_MINI_CODE_ID
        AdPlacement.Interstitial -> remote?.adInterstitialCode ?: UmengConfig.INTERSTITIAL_CODE_ID
        AdPlacement.Feed        -> remote?.adFreeCode ?: UmengConfig.FEED_CODE_ID
        AdPlacement.Reward,
        AdPlacement.TaskReward,
        AdPlacement.PureReward,
        AdPlacement.DownloadQuotaReward -> remote?.adRewardCode ?: UmengConfig.REWARD_CODE_ID
    }

    private fun resolveGdt(p: AdPlacement, remote: RemoteConfigData?) = when (p) {
        AdPlacement.Splash      -> remote?.adSplashCode ?: GdtConfig.SPLASH_POS_ID
        AdPlacement.Banner      -> remote?.adBannerCode ?: GdtConfig.BANNER_POS_ID
        AdPlacement.BannerMini  -> remote?.adMiniBannerCode ?: GdtConfig.BANNER_MINI_POS_ID
        AdPlacement.Interstitial -> remote?.adInterstitialCode ?: GdtConfig.INTERSTITIAL_POS_ID
        AdPlacement.Feed        -> remote?.adFreeCode ?: GdtConfig.FEED_POS_ID
        AdPlacement.Reward      -> remote?.adRewardCode ?: GdtConfig.REWARD_POS_ID
        AdPlacement.TaskReward  -> remote?.adTaskCode ?: GdtConfig.TASK_REWARD_POS_ID
        AdPlacement.PureReward  -> remote?.adPureCode ?: GdtConfig.PURE_REWARD_POS_ID
        AdPlacement.DownloadQuotaReward -> remote?.adDownloadCode ?: GdtConfig.DOWNLOAD_QUOTA_REWARD_POS_ID
    }

    private fun resolveBaidu(p: AdPlacement, remote: RemoteConfigData?) = when (p) {
        AdPlacement.Splash      -> remote?.adSplashCode ?: BaiduConfig.SPLASH_PLACE_ID
        AdPlacement.Banner      -> remote?.adBannerCode ?: BaiduConfig.BANNER_PLACE_ID
        AdPlacement.BannerMini  -> remote?.adMiniBannerCode ?: BaiduConfig.BANNER_MINI_PLACE_ID
        AdPlacement.Interstitial -> remote?.adInterstitialCode ?: BaiduConfig.INTERSTITIAL_PLACE_ID
        AdPlacement.Feed        -> remote?.adFreeCode ?: BaiduConfig.FEED_PLACE_ID
        AdPlacement.Reward,
        AdPlacement.TaskReward,
        AdPlacement.PureReward,
        AdPlacement.DownloadQuotaReward -> remote?.adRewardCode ?: BaiduConfig.REWARD_PLACE_ID
    }
}
