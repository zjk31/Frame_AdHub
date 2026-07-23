package cn.manxinghai.zhuimange.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.domain.model.AdLoadState
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import org.koin.compose.koinInject

/**
 * 内联信息流广告组件。
 * 纯净模式激活时自动隐藏。
 */
@Composable
fun InlineFeedAd(modifier: Modifier = Modifier) {
    val pureModeManager = koinInject<cn.manxinghai.zhuimange.core.PureModeManager>()
    val hideAds by pureModeManager.isPureModeActive.collectAsState(false)
    if (hideAds) return

    val adSdkManager = koinInject<AdSdkManager>()
    val adConfigRepo = koinInject<AdConfigRepository>()
    var adView by remember { mutableStateOf<View?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { adView = null } }

    LaunchedEffect(Unit) {
        try {
            val codeId = adConfigRepo.getCodeId(AdPlacement.Feed)
            if (codeId.isNotEmpty()) {
                adSdkManager.currentProvider.loadFeed(codeId, 1).collect { state ->
                    when (state) {
                        is AdLoadState.Loaded -> { adView = state.ad.firstOrNull(); isLoading = false }
                        is AdLoadState.Error -> { isLoading = false; hasError = true }
                        is AdLoadState.Loading -> { isLoading = true }
                        is AdLoadState.Idle -> {}
                    }
                }
            } else { return@LaunchedEffect }
        } catch (_: Exception) { return@LaunchedEffect }
    }

    if (adView == null) return
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color.DarkGray.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx)
                container.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                container.isClickable = false; container.isFocusable = false
                adView?.let { (it.parent as? ViewGroup)?.removeView(it); it.isClickable = true; container.addView(it) }
                container
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 内联迷你 Banner 广告（45dp，用于底部固定栏）。
 * 纯净模式激活时自动隐藏。
 */
@Composable
fun InlineMiniBannerAd(modifier: Modifier = Modifier) {
    val pureModeManager = koinInject<cn.manxinghai.zhuimange.core.PureModeManager>()
    val hideAds by pureModeManager.isPureModeActive.collectAsState(false)
    if (hideAds) return

    val adSdkManager = koinInject<AdSdkManager>()
    val adConfigRepo = koinInject<AdConfigRepository>()
    var adView by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(Unit) {
        try {
            val codeId = adConfigRepo.getCodeId(AdPlacement.BannerMini)
            if (codeId.isNotEmpty()) {
                adSdkManager.currentProvider.loadBanner(codeId, expressHeightDp = 45f).collect { state ->
                    when (state) {
                        is AdLoadState.Loaded -> { adView = state.ad }
                        is AdLoadState.Error -> {}
                        else -> {}
                    }
                }
            } else { return@LaunchedEffect }
        } catch (_: Exception) { return@LaunchedEffect }
    }
    if (adView == null) return

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        Modifier
            .shadow(8.dp, RectangleShape, clip = false)
            .background(Color(0xFF1A1A1A))
            .padding(bottom = navBarPadding)
            .then(modifier)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    val container = FrameLayout(ctx)
                    container.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    adView?.let { (it.parent as? ViewGroup)?.removeView(it); container.addView(it) }
                    container
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 内联标准 Banner 广告。
 * 纯净模式激活时自动隐藏。
 */
@Composable
fun InlineBannerAd(modifier: Modifier = Modifier) {
    val pureModeManager = koinInject<cn.manxinghai.zhuimange.core.PureModeManager>()
    val hideAds by pureModeManager.isPureModeActive.collectAsState(false)
    if (hideAds) return

    val adSdkManager = koinInject<AdSdkManager>()
    val adConfigRepo = koinInject<AdConfigRepository>()
    var adView by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(Unit) {
        try {
            val codeId = adConfigRepo.getCodeId(AdPlacement.Banner)
            if (codeId.isNotEmpty()) {
                adSdkManager.currentProvider.loadBanner(codeId).collect { state ->
                    if (state is AdLoadState.Loaded) adView = state.ad
                }
            } else { return@LaunchedEffect }
        } catch (_: Exception) { return@LaunchedEffect }
    }
    if (adView == null) return
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(Color.DarkGray.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx)
                container.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                container.isClickable = false; container.isFocusable = false
                adView?.let { (it.parent as? ViewGroup)?.removeView(it); it.isClickable = true; container.addView(it) }
                container
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
