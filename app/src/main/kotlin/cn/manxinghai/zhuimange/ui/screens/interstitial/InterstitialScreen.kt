package cn.manxinghai.zhuimange.ui.screens.interstitial

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterstitialScreen(
    onBack: () -> Unit,
    viewModel: InterstitialViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插屏广告") },
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
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 通道 + 代码位信息
            Card(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(24.dp))

            when {
                state.pureModeActive -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "✨ 纯净模式已激活",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("广告已屏蔽，观看纯净激励视频可获得免广告权益")
                        }
                    }
                }

                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "广告加载中，请稍候...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                state.finished -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "插屏流程结束",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("已展示: ${if (state.shown) "是" else "否"}")
                            if (state.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("提示: ${state.errorMessage}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { activity?.let { viewModel.showInterstitial(it) } }) {
                        Text("再试一次")
                    }
                }

                else -> {
                    // 初始状态
                    Button(
                        onClick = { activity?.let { viewModel.showInterstitial(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("加载并展示插屏广告")
                    }
                }
            }
        }
    }
}
