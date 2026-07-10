package cn.android.adhub.data.provider.csj

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo
import com.bytedance.sdk.openadsdk.mediation.init.MediationConfig
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig
import org.json.JSONObject

/** 穿山甲/Pangle 广告配置——AppId + 代码位常量。 */
object CsjConfig {
    const val APP_ID = "5850778"
    const val APP_NAME = "Frame_AdHub"

    /** GroMore 聚合代码位（来自后台配置） */
    const val SPLASH_CODE_ID = "19910070"
    const val BANNER_CODE_ID = "19910063"
    const val BANNER_MINI_CODE_ID = "19910063"
    const val AD_SLOT_REWARD_VIDEO = "19910071"
    const val AD_SLOT_TASK_REWARD_VIDEO = "19910071"
    const val AD_SLOT_PURE_REWARD_VIDEO = "19910071"
    const val AD_SLOT_DOWNLOAD_QUOTA_REWARD_VIDEO = "19910071"
    const val AD_SLOT_INTERSTITIAL = "19910054"
    const val AD_FREE_CODE_ID = "19910043"

    // ── 开屏自定义兜底（方案二）──
    // 当 GroMore 配置拉取失败时，直接用 pangle 代码位请求广告，避免无填充。
    // adnSlotId = 穿山甲平台上的开屏广告位 ID，与 GroMore 聚合 ID 不同。
    // 登录穿山甲后台 → 媒体管理 → 代码位管理 → 找到开屏代码位即可看到。
    private const val PANGLE_SPLASH_SLOT_ID = ""  // TODO: 填入穿山甲后台的开屏代码位

    /** 读取 GroMore 线上导出的配置（方案一：本地导入配置）。 */
    private fun loadLocalConfig(context: Context): JSONObject? {
        return try {
            val inputStream = context.assets.open(CONFIG_FILE)
            val configStr = inputStream.bufferedReader().use { it.readText() }
            JSONObject(configStr)
        } catch (e: Exception) {
            Log.w(TAG, "无法加载本地 GroMore 配置: ${e.message}（文件不存在则忽略）")
            null
        }
    }

    /** 构建 MediationConfig（含本地导入配置 + 开屏自定义兜底）。 */
    private fun buildMediationConfig(context: Context): MediationConfig {
        val builder = MediationConfig.Builder()

        // 方案一：本地导入配置（减少首次冷启动/网络异常导致的无填充）
        val localConfig = loadLocalConfig(context)
        if (localConfig != null) {
            builder.setCustomLocalConfig(localConfig)
            Log.i(TAG, "已注入本地 GroMore 配置")
        }

        return builder.build()
    }

    /**
     * 创建开屏广告的瀑布流兜底信息。
     * 对齐 flutter_merge：始终返回 MediationSplashRequestInfo（即使字段为空），
     * 确保 GroMore 正确识别 Pangle ADN 进行瀑布流排序。
     */
    fun buildSplashFallback(): MediationSplashRequestInfo {
        val slotId = PANGLE_SPLASH_SLOT_ID
        return object : MediationSplashRequestInfo(
            com.bytedance.sdk.openadsdk.mediation.MediationConstant.ADN_PANGLE,
            slotId,
            if (slotId.isNotBlank()) APP_ID else "",
            "",  // pangle 没有 appKey
        ) {}
    }

    /** 共享的 TTAdConfig 构建，AdHubApp 统一调用，避免配置不一致。
     * 对齐 flutter_merge：不设置 setMediationConfig（避免覆盖 GroMore 默认行为）。 */
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

    private const val CONFIG_FILE = "site_config_5850778"
    private const val TAG = "CsjConfig"
}
