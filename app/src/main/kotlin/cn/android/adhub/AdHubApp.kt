package cn.android.adhub

import android.app.Application
import android.util.Log
import cn.android.adhub.core.AppContextHolder

/**
 * Application — 仅保留全局上下文初始化，不再预加载广告。
 *
 * 对齐 flutter_merge 方案：广告加载完全放到 SplashAdActivity 中执行，
 * 避免 Application.onCreate 过重，同时杜绝 Application 阶段初始化失败
 * 导致的崩溃/白屏。
 */
class AdHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 优先尝试 IPv4，规避部分网络环境下 IPv6 无法连通穿山甲服务器的问题
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")

        Log.i("AdHubApp", "onCreate")
        AppContextHolder.init(this)
    }
}
