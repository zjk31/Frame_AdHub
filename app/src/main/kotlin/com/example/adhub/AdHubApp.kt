package com.example.adhub

import android.app.Application
import com.example.adhub.core.AppContextHolder
import com.example.adhub.data.AdSdkManager
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

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)

        // 1. 启动 Koin DI 容器
        startKoin {
            androidContext(this@AdHubApp)
            modules(adModule)
        }

        // 2. 异步初始化当前通道的广告 SDK
        //    放在 IO 线程避免阻塞主线程，Koin 已就绪可安全获取依赖
        appScope.launch {
            try {
                val manager = GlobalContext.get().get<AdSdkManager>()
                manager.ensureProviderReady()
            } catch (e: Exception) {
                android.util.Log.e("AdHubApp", "SDK 初始化失败", e)
            }
        }

        // 3. 启动热启动插屏监听
        try {
            val hotStartManager = GlobalContext.get().get<com.example.adhub.core.HotStartInterstitialManager>()
            hotStartManager.start()
        } catch (e: Exception) {
            android.util.Log.w("AdHubApp", "热启动插屏管理器启动失败", e)
        }
    }
}
