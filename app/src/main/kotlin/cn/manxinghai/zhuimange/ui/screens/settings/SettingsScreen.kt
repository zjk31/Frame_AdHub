package cn.manxinghai.zhuimange.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===================== 广告设置 =====================
            SettingsGroupCard(title = "广告设置") {
                SwitchRow(
                    icon = Icons.Default.Campaign,
                    title = "广告展示",
                    subtitle = "关闭后所有广告将不再显示",
                    checked = ui.adEnabled,
                    onCheckedChange = viewModel::setAdEnabled
                )
            }

            // ===================== 下载设置 =====================
            SettingsGroupCard(title = "下载设置") {
                SwitchRow(
                    icon = Icons.Default.Download,
                    title = "自动下载新章节",
                    subtitle = "收藏的漫画更新时自动下载",
                    checked = ui.autoDownload,
                    onCheckedChange = viewModel::setAutoDownload
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SwitchRow(
                    icon = Icons.Default.Wifi,
                    title = "仅 Wi-Fi 下载",
                    subtitle = "移动网络下不下载图片",
                    checked = ui.downloadWifiOnly,
                    onCheckedChange = viewModel::setDownloadWifiOnly
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                // 下载缓存
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearDownloadCache() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("清除下载缓存", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "当前占用 ${formatSize(ui.downloadSizeBytes)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    if (ui.cacheCleared) {
                        Text("已清除", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                }
            }

            // ===================== 阅读设置 =====================
            SettingsGroupCard(title = "阅读设置") {
                SwitchRow(
                    icon = Icons.Default.DarkMode,
                    title = "默认夜间模式",
                    subtitle = "打开阅读器时自动切换夜间模式",
                    checked = ui.nightModeDefault,
                    onCheckedChange = viewModel::setNightModeDefault
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                // 自动翻页间隔
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动翻页间隔", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${ui.autoScrollInterval} 秒/页", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row {
                        IconButton(
                            onClick = {
                                if (ui.autoScrollInterval > 1) {
                                    viewModel.setAutoScrollInterval(ui.autoScrollInterval - 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Remove, "减少")
                        }
                        IconButton(
                            onClick = {
                                if (ui.autoScrollInterval < 10) {
                                    viewModel.setAutoScrollInterval(ui.autoScrollInterval + 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, "增加")
                        }
                    }
                }
            }

            // ===================== 消息推送 =====================
            SettingsGroupCard(title = "消息推送") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SSE 连接状态", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (ui.sseConnected) "已连接" else "未连接",
                            fontSize = 12.sp,
                            color = if (ui.sseConnected) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                    TextButton(onClick = { viewModel.reconnectSse() }) {
                        Text("重连", fontSize = 13.sp)
                    }
                }
            }

            // ===================== 关于 =====================
            SettingsGroupCard(title = "关于") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("版本", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "1.0.0",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val df = DecimalFormat("#.##")
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024))} MB"
        else -> "${df.format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}
