package cn.android.adhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.android.adhub.ui.AdHubApp
import org.koin.android.ext.android.inject

/**
 * 单 Activity 架构。
 *
 * 冷启动由 [SplashAdActivity] 管理（系统启动页 → 广告 → 跳转本 Activity）。
 * 本 Activity 使用 [LaunchTheme] 确保 Window 创建时立即绘制 #F5F5F5 背景，
 * 与 Compose [AdHubTheme] 的 background 色值一致，消除 Activity 切换白屏。
 */
class MainActivity : ComponentActivity() {

    private val hotStartManager: cn.android.adhub.core.HotStartInterstitialManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdHubApp()
        }
        hotStartManager.bindActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        hotStartManager.unbindActivity(this)
    }
}
