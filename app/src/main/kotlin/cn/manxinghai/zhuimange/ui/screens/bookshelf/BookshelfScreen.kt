package cn.manxinghai.zhuimange.ui.screens.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.manxinghai.zhuimange.domain.model.FavoriteItem
import cn.manxinghai.zhuimange.domain.model.ReadingHistory
import cn.manxinghai.zhuimange.ui.theme.AdHubTheme
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onMangaClick: (Long) -> Unit,
    onChapterClick: (Long, Long) -> Unit,
    viewModel: BookshelfViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Tab
        TabRow(
            selectedTabIndex = ui.tab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = ui.tab == BookshelfTab.FAVORITES,
                onClick = { viewModel.switchTab(BookshelfTab.FAVORITES) },
                text = { Text("我的收藏") }
            )
            Tab(
                selected = ui.tab == BookshelfTab.HISTORY,
                onClick = { viewModel.switchTab(BookshelfTab.HISTORY) },
                text = { Text("阅读历史") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                ui.isLoading -> LoadingIndicator()
                ui.tab == BookshelfTab.FAVORITES -> FavoritesList(
                    favorites = ui.favorites,
                    unreadCounts = ui.unreadChapterCounts,
                    onMangaClick = onMangaClick,
                    onDelete = viewModel::deleteFavorite
                )
                ui.tab == BookshelfTab.HISTORY -> HistoryList(
                    history = ui.history,
                    onMangaClick = onMangaClick,
                    onChapterClick = onChapterClick,
                    onClear = viewModel::clearHistory
                )
            }
        }
    }
}

@Composable
private fun FavoritesList(
    favorites: List<FavoriteItem>,
    unreadCounts: Map<Long, Int> = emptyMap(),
    onMangaClick: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Default.MenuBook,
            message = "暂无收藏，去看看喜欢的漫画吧"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites, key = { it.mangaId }) { item ->
                FavoriteItemRow(
                    item = item,
                    unreadCount = unreadCounts[item.mangaId] ?: 0,
                    onClick = { onMangaClick(item.mangaId) },
                    onDelete = { onDelete(item.mangaId) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteItemRow(
    item: FavoriteItem,
    unreadCount: Int = 0,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除《${item.mangaName}》的收藏吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.mangaName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.mangaName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.author.ifBlank { "未知作者" },
                fontSize = 11.sp,
                color = Color.Gray
            )
            if (item.lastUpdateChapterName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "更新至：${item.lastUpdateChapterName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
        IconButton(onClick = { showDeleteDialog = true }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HistoryList(
    history: List<ReadingHistory>,
    onMangaClick: (Long) -> Unit,
    onChapterClick: (Long, Long) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClear) {
                    Text("清空历史", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                message = "暂无阅读记录"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history, key = { it.mangaId }) { item ->
                    HistoryItemRow(
                        item = item,
                        onClick = { onMangaClick(item.mangaId) },
                        onChapterClick = { onChapterClick(item.mangaId, item.lastChapterId) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    item: ReadingHistory,
    onClick: () -> Unit,
    onChapterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.mangaName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.mangaName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "读到：${item.lastChapterName}",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "第${item.lastReadPage}页 · 共${item.chapterCount}话",
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
        TextButton(onClick = onChapterClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("继续", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
        Icon(
            icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
