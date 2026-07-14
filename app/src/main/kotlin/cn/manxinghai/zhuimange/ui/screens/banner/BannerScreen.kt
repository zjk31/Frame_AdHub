package cn.manxinghai.zhuimange.ui.screens.banner

import android.app.Activity
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.manxinghai.zhuimange.domain.model.AdLoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerScreen(
    onBack: () -> Unit,
    viewModel: BannerViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 将 Activity Context 注入 ViewModel（GDT/百度 Banner 需要）
    val context = LocalContext.current
    LaunchedEffect(context) {
        viewModel.setActivity(context as? Activity)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setActivity(null) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("横幅广告") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 通道 + 代码位信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "当前通道: ${state.channelName}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "代码位: ${state.codeId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 重试按钮
            if (state.adState is AdLoadState.Error) {
                OutlinedButton(
                    onClick = { viewModel.loadBanner() },
                ) {
                    Text("重新加载")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 纯净模式提示
            if (state.pureModeActive) {
                PureModeBanner()
            }

            // 广告区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (val ad = state.adState) {
                    is AdLoadState.Idle -> {}
                    is AdLoadState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    }
                    is AdLoadState.Loaded -> {
                        BannerAdView(adView = ad.ad)
                    }
                    is AdLoadState.Error -> {
                        Text(
                            "广告加载失败: ${ad.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerAdView(adView: View) {
    AndroidView(
        factory = { adView },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    )
}

@Composable
private fun PureModeBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Text(
            "✨ 纯净模式已激活，广告已屏蔽",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
