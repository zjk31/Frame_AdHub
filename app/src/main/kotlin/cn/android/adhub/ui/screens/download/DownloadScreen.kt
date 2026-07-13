package cn.android.adhub.ui.screens.download

import androidx.compose.animation.*
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
import cn.android.adhub.data.local.DownloadTaskEntity
import cn.android.adhub.data.local.DownloadedChapterEntity
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onBack: () -> Unit,
    viewModel: DownloadViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的下载", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (ui.completedTasks.isNotEmpty() || ui.activeTasks.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllDownloads() }) {
                            Icon(Icons.Default.DeleteSweep, "清空")
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
            // 下载配额卡片
            DownloadQuotaCard(
                quota = ui.downloadQuota,
                stats = ui.downloadStats,
                downloadSizeBytes = ui.downloadSizeBytes
            )

            // Tab：下载中 / 已完成
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("下载中 (${ui.activeTasks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("已完成 (${ui.completedTasks.size})") }
                )
            }

            when (selectedTab) {
                0 -> DownloadTaskList(
                    tasks = ui.activeTasks,
                    onPause = viewModel::pauseDownload,
                    onResume = viewModel::resumeDownload,
                    onCancel = viewModel::cancelDownload
                )
                1 -> DownloadedChapterList(
                    chapters = ui.downloadedChapters,
                    onDelete = { viewModel.deleteMangaDownloads(it) }
                )
            }
        }
    }
}

@Composable
private fun DownloadQuotaCard(
    quota: cn.android.adhub.domain.model.DownloadQuota?,
    stats: cn.android.adhub.data.download.DownloadStats,
    downloadSizeBytes: Long
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "下载配额",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (quota != null) "${quota.remaining}/${quota.total}" else "--",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "已下载",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${stats.mangaCount} 部 · ${stats.chapterCount} 章",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatFileSize(downloadSizeBytes),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            if (quota != null && quota.total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { quota.used.toFloat() / quota.total },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadTaskList(
    tasks: List<DownloadTaskEntity>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(icon = Icons.Default.Download, message = "暂无下载任务")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.chapterId }) { task ->
                DownloadTaskItem(
                    task = task,
                    onPause = { onPause(task.chapterId) },
                    onResume = { onResume(task.chapterId) },
                    onCancel = { onCancel(task.chapterId) }
                )
            }
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: DownloadTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.chapterName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.mangaName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 状态标签
                when (task.status) {
                    DownloadTaskEntity.STATUS_DOWNLOADING -> {
                        Text("${task.progress}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, "暂停", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    DownloadTaskEntity.STATUS_QUEUED -> {
                        Text("排队中", fontSize = 12.sp, color = Color.Gray)
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, "取消", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    DownloadTaskEntity.STATUS_PAUSED -> {
                        Text("已暂停", fontSize = 12.sp, color = Color.Gray)
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, "继续", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    DownloadTaskEntity.STATUS_FAILED -> {
                        Text("失败", fontSize = 12.sp, color = Color.Red)
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.Refresh, "重试", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "取消", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            // 进度条
            if (task.status == DownloadTaskEntity.STATUS_DOWNLOADING || task.status == DownloadTaskEntity.STATUS_PAUSED) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (task.status == DownloadTaskEntity.STATUS_FAILED && task.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.errorMessage,
                    fontSize = 11.sp,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun DownloadedChapterList(
    chapters: List<DownloadedChapterEntity>,
    onDelete: (Long) -> Unit
) {
    if (chapters.isEmpty()) {
        EmptyState(icon = Icons.Default.DownloadDone, message = "暂无已下载的章节")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chapters, key = { it.chapterId }) { chapter ->
                DownloadedChapterItem(
                    chapter = chapter,
                    onDelete = { onDelete(chapter.mangaId) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedChapterItem(
    chapter: DownloadedChapterEntity,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.chapterName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${chapter.mangaName} · ${chapter.downloadedPages}/${chapter.totalPages}页 · ${formatTime(chapter.downloadTime)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = Color.Gray, modifier = Modifier.size(20.dp))
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

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
