package cn.manxinghai.zhuimange.data.remote

/**
 * 广告远程配置数据模型。
 *
 * 子 App 从自己的后端拉取配置后填充此对象，写入 SharedPreferences 供骨架读取。
 * 骨架本身不包含网络请求实现——所有字段均可为 null，表示"未下发"。
 */
data class RemoteConfigData(
    // ── 通道选择 ──
    val adType: Int?,          // 0=友盟, 1=穿山甲, 2=优量汇, 3=百度

    // ── 广告总开关（0=禁用, 1=启用）──
    val adStatus: Int?,

    // ── 各广告位独立开关（1=启用/0=禁用）──
    val adBannerStatus: Int?,
    val adIntersStatus: Int?,
    val adRewardStatus: Int?,
    val adSplashStatus: Int?,
    val adFreeStatus: Int?,

    // ── 各广告位代码（服务端下发优先于本地硬编码）──
    val adSplashCode: String?,
    val adBannerCode: String?,
    val adMiniBannerCode: String?,
    val adRewardCode: String?,
    val adTaskCode: String?,
    val adPureCode: String?,
    val adDownloadCode: String?,
    val adInterstitialCode: String?,
    val adFreeCode: String?,
)
