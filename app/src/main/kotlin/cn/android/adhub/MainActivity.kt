package cn.android.adhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.android.adhub.di.adModule
import cn.android.adhub.di.mangaModule
import cn.android.adhub.ui.AdHubApp
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 漫画 App 主界面（备用入口）。
 *
 * 正常用户流程由 [SplashAdActivity]（LAUNCHER）承载：
 * SplashAdActivity 内嵌的 ComposeView 同样渲染 [AdHubApp]。
 * 本 Activity 作为 adb 调试入口或 SplashAdActivity 降级跳转目标。
 */
class MainActivity : ComponentActivity(), KoinComponent {

    private val messageRepository: cn.android.adhub.data.sse.MessageRepository by inject()
    private val appUpdateChecker: cn.android.adhub.data.update.AppUpdateChecker by inject()
    private val userSession: cn.android.adhub.data.repository.UserSession by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Koin（如果尚未初始化）
        initKoin()

        // 后台加载用户会话 + SSE + 版本检查
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
        messageRepository.disconnect()
    }

    private fun initKoin() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(adModule, mangaModule)
            }
        }
    }
}
