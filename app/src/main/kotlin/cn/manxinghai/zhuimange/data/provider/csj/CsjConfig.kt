package cn.manxinghai.zhuimange.data.provider.csj

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig

/** 穿山甲/Pangle 广告配置 — GroMore 聚合模式（对齐 flutter_merge）。 */
object CsjConfig {
    const val APP_ID = "5852679"
    const val APP_NAME = "Frame_AdHub"

    /** 服务器 adsign.manxinghai.cn 按包名 cn.manxinghai.zhuimange 下发的代码位 */
    const val SPLASH_CODE_ID = "104237165"
    const val BANNER_CODE_ID = "104246804"
    const val BANNER_MINI_CODE_ID = "104246804"
    const val AD_SLOT_REWARD_VIDEO = "104244781"
    const val AD_SLOT_TASK_REWARD_VIDEO = "104246806"
    const val AD_SLOT_PURE_REWARD_VIDEO = "104246339"
    const val AD_SLOT_DOWNLOAD_QUOTA_REWARD_VIDEO = "104244399"
    const val AD_SLOT_INTERSTITIAL = "104246719"
    const val AD_FREE_CODE_ID = "104246621"

    /** 构建 TTAdConfig — GroMore 聚合模式，对齐 flutter_merge。 */
    fun buildAdConfig(context: Context): TTAdConfig = TTAdConfig.Builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .useMediation(true)
        .debug(true)
        .supportMultiProcess(false)
        .customController(object : TTCustomController() {
            override fun isCanUseLocation(): Boolean = true
            override fun isCanUsePhoneState(): Boolean = true
            override fun isCanUseWifiState(): Boolean = true
            override fun isCanUseWriteExternal(): Boolean = true
            override fun isCanUseAndroidId(): Boolean = true
            override fun alist(): Boolean = false
            override fun getMediationPrivacyConfig(): MediationPrivacyConfig {
                return object : MediationPrivacyConfig() {
                    override fun isLimitPersonalAds(): Boolean = false
                    override fun isProgrammaticRecommend(): Boolean = true
                }
            }
        })
        .build()

    private const val TAG = "CsjConfig"
}
