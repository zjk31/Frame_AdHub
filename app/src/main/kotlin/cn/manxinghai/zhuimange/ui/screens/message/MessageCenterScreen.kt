package cn.manxinghai.zhuimange.ui.screens.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.manxinghai.zhuimange.data.sse.SseConnectionState
import cn.manxinghai.zhuimange.domain.model.SystemMessage
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(
    onBack: () -> Unit,
    viewModel: MessageViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (ui.messages.isNotEmpty()) {
                        IconButton(onClick = viewModel::markAllRead) {
                            Icon(Icons.Default.DoneAll, "全部已读")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 连接状态指示
            ConnectionStatusBar(state = ui.connectionState)

            when {
                ui.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                ui.messages.isEmpty() -> {
                    EmptyState(icon = Icons.Default.Notifications, message = "暂无消息")
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.messages, key = { it.id }) { msg ->
                            MessageItem(
                                message = msg,
                                onClick = { viewModel.markAsRead(msg.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(state: SseConnectionState) {
    val (color, text) = when (state) {
        SseConnectionState.CONNECTED -> Color(0xFF4CAF50) to "已连接"
        SseConnectionState.CONNECTING -> Color(0xFFFFA726) to "连接中..."
        SseConnectionState.FAILED -> Color.Red to "连接失败，正在重试"
        SseConnectionState.DISCONNECTED -> Color.Gray to "未连接"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = color)
    }
}

@Composable
private fun MessageItem(
    message: SystemMessage,
    onClick: () -> Unit
) {
    val typeIcon = when (message.type) {
        "announcement" -> Icons.Default.Campaign
        "reward" -> Icons.Default.CardGiftcard
        "system" -> Icons.Default.Info
        else -> Icons.Default.Notifications
    }
    val typeColor = when (message.type) {
        "announcement" -> Color(0xFFFF6B35)
        "reward" -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatTime(message.createTime),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = Color.Gray, fontSize = 14.sp)
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> "${diff / 86400_000}天前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
