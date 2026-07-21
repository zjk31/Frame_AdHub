package cn.manxinghai.zhuimange

import android.app.Application
import android.util.Log
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.di.adModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Application 入口。
 *
 * 初始化顺序：
 *   1. Koin DI 容器（主线程，确保 SplashAdActivity 可用）
 *   2. 广告 SDK 预热（后台协程）
 *   3. 热启动插屏监听（Application 级 ActivityLifecycleCallbacks）
 *
 * 子 App 接入时在 [startKoin] 的 [modules] 里追加自己的模块，
 * 并在 [onCreate] 末尾调用自己的初始化逻辑。
 */
class AdHubApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i("AdHubApp", "onCreate — 开始初始化")

        // ── 0. Context 持有 ──
        AppContextHolder.init(this)

        // ── 1. Koin DI 容器 ──
        // 主线程启动，确保 SplashAdActivity 能立即拿到 AdSdkManager。
        // 子 App 在这里追加自己的 module，如 modules(adModule, myBusinessModule)
        startKoin {
            androidContext(this@AdHubApp)
            modules(adModule)
        }
        Log.i("AdHubApp", "Koin 初始化完成")

        // ── 2. 广告 SDK 预热（后台，不阻塞启动） ──
        // 对齐 flutter_merge：不在 Application 中提前调用 TTAdSdk.init，
        // 统一由 CsjAdProvider.ensureReady() 在 SplashAdActivity 启动后初始化。
        appScope.launch {
            try {
                val manager = GlobalContext.get().get<AdSdkManager>()
                Log.i("AdHubApp", "ensureProviderReady 开始…")
                manager.ensureProviderReady()
                Log.i("AdHubApp", "ensureProviderReady 完成")
            } catch (e: Exception) {
                Log.e("AdHubApp", "SDK 预热失败", e)
            }
        }

        // ── 3. 热启动插屏监听（Application 级，全局生效） ──
        try {
            val hotStartManager =
                GlobalContext.get().get<cn.manxinghai.zhuimange.core.HotStartInterstitialManager>()
            hotStartManager.start()
            Log.i("AdHubApp", "热启动插屏监听已启动")
        } catch (e: Exception) {
            Log.w("AdHubApp", "热启动插屏监听启动失败", e)
        }
    }
}
