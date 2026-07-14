package cn.manxinghai.zhuimange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.manxinghai.zhuimange.di.adModule
import cn.manxinghai.zhuimange.di.mangaModule
import cn.manxinghai.zhuimange.ui.AdHubApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules

/**
 * 漫画 App 主页。
 * SplashAdActivity 广告结束后跳转至此。
 *
 * Koin 在此初始化（广告模块 + 漫画模块），SplashAdActivity 不依赖 Koin。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Koin（广告模块 + 漫画模块）
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(adModule, mangaModule)
            }
        } else {
            loadKoinModules(mangaModule)
        }

        enableEdgeToEdge()

        val userSession = GlobalContext.get().get<cn.manxinghai.zhuimange.data.repository.UserSession>()
        val messageRepository = GlobalContext.get().get<cn.manxinghai.zhuimange.data.sse.MessageRepository>()
        val appUpdateChecker = GlobalContext.get().get<cn.manxinghai.zhuimange.data.update.AppUpdateChecker>()

        // 后台：用户会话 + SSE + 版本检查
        MainScope().launch(Dispatchers.IO) {
            userSession.load()
            messageRepository.init()
            appUpdateChecker.check(autoCheck = true)
        }

        setContent {
            AdHubApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val messageRepository = GlobalContext.getOrNull()?.get<cn.manxinghai.zhuimange.data.sse.MessageRepository>()
            messageRepository?.disconnect()
        } catch (e: Exception) {
            // Koin 可能未初始化
        }
    }
}
