package cn.manxinghai.zhuimange.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cn.manxinghai.zhuimange.core.PureModeManager
import org.koin.compose.koinInject

/**
 * 纯净模式广告门禁 —— 对齐 Flutter PureModeAdGate
 *
 * 当纯净模式激活时，隐藏 Banner / 信息流 / 插屏等所有广告。
 */
@Composable
fun PureModeAdGate(
    content: @Composable () -> Unit
) {
    val pureModeManager = koinInject<PureModeManager>()
    val hideAds by pureModeManager.isPureModeActive.collectAsState(false)

    if (!hideAds) {
        content()
    }
}
