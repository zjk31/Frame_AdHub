package cn.android.adhub.ui.screens.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout

/**
 * 广告容器叠层 — 用 AndroidView 包裹 FrameLayout
 * CSJSplashAd.showSplashView(container) 需要 ViewGroup
 */
@Composable
fun SplashAdOverlay(
    onContainerReady: (FrameLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val container = remember { FrameLayout(context) }

    AndroidView(
        factory = {
            container.apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            onContainerReady(container)
            container
        },
        modifier = modifier.fillMaxSize()
    )
}
