package cn.manxinghai.zhuimange.data.provider.csj

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig

/** 穿山甲/Pangle 广告配置 — GroMore 聚合模式（对齐 flutter_merge）。 */
object CsjConfig {
    const val APP_ID = "5850778"
    const val APP_NAME = "Frame_AdHub"

    /** GroMore 聚合代码位（远程配置下发，以 104 开头） */
    const val SPLASH_CODE_ID = "19910070"
    const val BANNER_CODE_ID = "19910063"
    const val BANNER_MINI_CODE_ID = "19910063"
    const val AD_SLOT_REWARD_VIDEO = "19910071"
    const val AD_SLOT_TASK_REWARD_VIDEO = "19910071"
    const val AD_SLOT_PURE_REWARD_VIDEO = "19910071"
    const val AD_SLOT_DOWNLOAD_QUOTA_REWARD_VIDEO = "19910071"
    const val AD_SLOT_INTERSTITIAL = "19910054"
    const val AD_FREE_CODE_ID = "19910043"

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
