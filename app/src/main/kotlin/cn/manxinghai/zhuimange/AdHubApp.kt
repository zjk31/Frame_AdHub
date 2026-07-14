package cn.manxinghai.zhuimange

import android.app.Application
import android.util.Log
import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.core.MangaImageFetcher
import coil3.ImageLoader
import coil3.SingletonImageLoader

class AdHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv4Addresses", "true")

        Log.i("AdHubApp", "onCreate")

        // Coil 3 图片加载初始化
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components { add(MangaImageFetcher.Factory()) }
                .build()
        }

        AppContextHolder.init(this)
    }
}
