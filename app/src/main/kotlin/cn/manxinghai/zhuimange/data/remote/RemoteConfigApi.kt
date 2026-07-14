package cn.manxinghai.zhuimange.data.remote

import retrofit2.http.GET

/**
 * 远程配置 API。
 *
 * - [getGlobalSetting]：获取 adType（通道选择）
 * - 完整广告配置见 [cn.manxinghai.zhuimange.data.repository.AdConfigRepositoryImpl.fetchAdConfig]
 */
interface RemoteConfigApi {

    @GET("globalSetting/get")
    suspend fun getGlobalSetting(): RemoteConfigResponse
}

data class RemoteConfigResponse(
    val code: Int,
    val data: RemoteConfigData?,
    val message: String?,
)

data class RemoteConfigData(
    // ── 通道选择 ──
    val adType: Int?,          // 0=友盟, 1=穿山甲, 2=优量汇, 3=百度

    // ── SDK AppKey/AppId ──
    val adCode: String?,

    // ── 广告开关 ──
    val adStatus: Int?,        // 0=禁用, 1=启用

    // ── 各广告位代码（服务端下发，优先于本地硬编码） ──
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
