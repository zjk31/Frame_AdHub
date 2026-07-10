package cn.android.adhub.ui.screens.splash

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * 冷启动入口 —— 不渲染任何 UI，仅负责触发开屏广告。
 *
 * 视觉上，系统启动页（LaunchTheme 的 #F5F5F5 背景 + App 图标）是唯一的启动画面。
 * 本 Composable 在系统启动页淡出后方才出现：
 * 1. 背景色跟系统启动页一致 → 视觉无缝
 * 2. 若广告未就绪 → 背景停留，等待广告加载
 * 3. 广告展示完毕 / 纯净模式 / 异常 → 跳转首页
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onEnterHome: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    // 广告流程走完后跳首页
    LaunchedEffect(state.resolved) {
        if (state.resolved) {
            delay(200)
            onEnterHome()
        }
    }

    // 自动触发冷启动流程
    LaunchedEffect(Unit) {
        activity?.let { viewModel.startColdStartFlow(it) }
    }

    // 无任何 UI 文字/图标 — 仅保留与系统启动页一致的纯色背景，
    // 用于承接系统启动页淡出后的短暂过渡
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}
