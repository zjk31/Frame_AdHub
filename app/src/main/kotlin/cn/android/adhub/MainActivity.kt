package cn.android.adhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cn.android.adhub.ui.AdHubApp
import org.koin.android.ext.android.inject

/**
 * 单 Activity 架构。
 *
 * 系统启动页由 [LaunchTheme]（#F5F5F5 背景 + App 图标）统一管理：
 * - API 31+：平台 SplashScreen API 自动展示，首帧渲染后淡出
 * - pre-API 31：windowBackground 方案
 *
 * 我们的 [SplashScreen] composable 渲染同一背景色，与系统启动页视觉无缝衔接，
 * 用户感知为「一次连续启动」。
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
