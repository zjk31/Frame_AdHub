package cn.android.adhub

import android.app.Application
import android.util.Log
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.data.provider.csj.CsjConfig
import cn.android.adhub.di.adModule
import cn.android.adhub.di.mangaModule
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
        Log.i("AdHubApp", "onCreate — 开始初始化")
        AppContextHolder.init(this)

        // 对齐 flutter_merge：不在 Application 中提前调用 TTAdSdk.init，
        // 统一由 CsjAdProvider.ensureReady() 在 SplashAdActivity 启动后初始化。
        // 避免 ContentProvider + Application + Provider 三重初始化导致 SDK 状态混乱。

        // 1. 启动 Koin DI 容器
        startKoin {
            androidContext(this@AdHubApp)
            modules(adModule, mangaModule)
        }

        // 2. 异步调用 ensureProviderReady() 使 SDK 完全就绪
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
            val hotStartManager = GlobalContext.get().get<cn.android.adhub.core.HotStartInterstitialManager>()
            hotStartManager.start()
        } catch (e: Exception) {
            Log.w("AdHubApp", "热启动插屏管理器启动失败", e)
        }
    }
}
