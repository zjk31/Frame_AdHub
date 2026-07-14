package cn.manxinghai.zhuimange.domain.model

/**
 * 广告位类型，对应各 SDK 中的不同展示形式。
 */
enum class AdPlacement(val slotKey: String) {
    Splash("splash"),
    Banner("banner"),
    BannerMini("bannerMini"),
    Feed("feed"),
    Interstitial("interstitial"),
    Reward("reward"),
    TaskReward("taskReward"),
    PureReward("pureReward"),
    DownloadQuotaReward("downloadQuotaReward"),
}
