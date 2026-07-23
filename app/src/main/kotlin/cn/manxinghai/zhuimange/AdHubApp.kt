package cn.manxinghai.zhuimange

import android.app.Application
import android.util.Log
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.core.DeviceIdManager
import cn.manxinghai.zhuimange.core.InviteCodeManager
import cn.manxinghai.zhuimange.di.adModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application 入口。
 *
 * 初始化顺序：
 *   1. Koin DI 容器（后台线程，不阻塞 SplashAdActivity 启动）
 *   2. 设备标识 + 邀请码管理器
 *   3. 远程广告配置预拉取（异步，供 SplashAdActivity 立即读取缓存）
 *
 * 子 App 接入时在 [startKoin] 的 [modules] 里追加自己的模块，
 * 并在 [onCreate] 末尾调用自己的初始化逻辑。
 */
class AdHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")

        Log.i("AdHubApp", "onCreate")

        // Koin 后台初始化（利用 SplashAdActivity 广告窗口，不阻塞主线程）
        CoroutineScope(Dispatchers.IO).launch {
            startKoin { androidContext(this@AdHubApp); modules(adModule) }
            Log.i("AdHubApp", "Koin 初始化完成")
        }

        AppContextHolder.init(this)
        DeviceIdManager.init(this)
        InviteCodeManager.init(this)
    }
}
