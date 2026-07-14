package cn.maga.lingdongmanhua

import android.app.Application
import android.util.Log
import cn.maga.lingdongmanhua.core.AppContextHolder

/**
 * Application — 极轻量，仅上下文初始化。SDK init 全部在 Activity 主线程执行。
 * 对齐 flutter_merge：Application 不初始化任何广告 SDK。
 */
class AdHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")

        Log.i("AdHubApp", "onCreate")
        AppContextHolder.init(this)
    }
}
