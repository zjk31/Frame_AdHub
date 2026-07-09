package com.example.adhub

import android.app.Application
import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig
import com.example.adhub.core.AppContextHolder
import com.example.adhub.data.AdSdkManager
import com.example.adhub.data.provider.csj.CsjConfig
import com.example.adhub.di.adModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class AdHubApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 在 ContentProvider 自动初始化之前完成 CSJ SDK init，
        // 否则 SDK 会被 ContentProvider 用空配置初始化，导致 getAppId=null、40006。
        initCsjSdkEarly(base ?: this)
    }

    private fun initCsjSdkEarly(context: Context) {
        try {
            val config = TTAdConfig.Builder()
                .appId(CsjConfig.APP_ID)
                .appName("Frame_AdHub")
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
            TTAdSdk.init(context.applicationContext, config)
            Log.i("AdHubApp", "CSJ SDK early init done, appId=${CsjConfig.APP_ID}")
        } catch (e: Exception) {
            Log.e("AdHubApp", "CSJ early init failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("AdHubApp", "onCreate — 开始初始化")
        AppContextHolder.init(this)

        // 1. 启动 Koin DI 容器
        startKoin {
            androidContext(this@AdHubApp)
            modules(adModule)
        }

        // 2. 异步调用 start() 使 SDK 就绪
        appScope.launch {
            try {
                val manager = GlobalContext.get().get<AdSdkManager>()
                Log.i("AdHubApp", "ensureProviderReady 开始…")
                manager.ensureProviderReady()
                Log.i("AdHubApp", "ensureProviderReady 完成")
            } catch (e: Exception) {
                Log.e("AdHubApp", "SDK 初始化失败", e)
            }
        }

        // 3. 启动热启动插屏监听
        try {
            val hotStartManager = GlobalContext.get().get<com.example.adhub.core.HotStartInterstitialManager>()
            hotStartManager.start()
        } catch (e: Exception) {
            Log.w("AdHubApp", "热启动插屏管理器启动失败", e)
        }
    }
}
