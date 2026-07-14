package cn.manxinghai.zhuimange.ui.screens.reward

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardScreen(
    slotKey: String,
    onBack: () -> Unit,
    viewModel: RewardViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LaunchedEffect(slotKey) {
        viewModel.loadReward(slotKey)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(slotLabel(slotKey)) },
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
                    Text(
                        "场景: ${slotLabel(slotKey)} (${state.slotKey})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 加载 / 结果显示
            when {
                state.isLoading && state.result == null -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "广告加载中，请稍候...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                state.errorMessage != null && state.result == null -> {
                    // 预加载阶段出错（没走到展示流程）
                    Text(
                        "加载失败: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { activity?.let { viewModel.startReward(it) } }) {
                        Text("重新加载")
                    }
                }

                state.result != null -> {
                    // 展示流程已结束
                    RewardResultCard(result = state.result!!)
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { activity?.let { viewModel.startReward(it) } }) {
                        Text("再试一次")
                    }
                }
            }

            if (state.result == null && !state.isLoading && state.errorMessage == null) {
                // 初始状态 — 还未点击加载
                Button(
                    onClick = { activity?.let { viewModel.startReward(it) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("开始播放激励视频")
                }
            }
        }
    }
}

@Composable
private fun RewardResultCard(result: cn.manxinghai.zhuimange.domain.model.RewardResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.rewardGranted)
                Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "广告流程完成",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ResultRow("已展示", if (result.shown) "是" else "否")
            ResultRow("已结束", if (result.finished) "是" else "否")
            ResultRow("激励发放", if (result.rewardGranted) "✅ 已发放" else "❌ 未发放")

            if (!result.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ResultRow("错误信息", result.errorMessage)
            }

            if (result.extra.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("额外信息:", style = MaterialTheme.typography.labelMedium)
                result.extra.forEach { (key, value) ->
                    ResultRow("  $key", value?.toString() ?: "null")
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun slotLabel(slotKey: String): String = when (slotKey) {
    "taskReward" -> "任务激励"
    "pureReward" -> "纯净激励"
    "downloadQuotaReward" -> "下载配额激励"
    "reward" -> "激励视频"
    else -> "激励视频"
}
