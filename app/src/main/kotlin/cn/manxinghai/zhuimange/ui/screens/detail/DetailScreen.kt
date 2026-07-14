package cn.manxinghai.zhuimange.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.manxinghai.zhuimange.domain.model.Chapter
import cn.manxinghai.zhuimange.domain.model.Manga
import cn.manxinghai.zhuimange.domain.model.MangaStatus
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mangaId: Long,
    onBack: () -> Unit,
    onChapterClick: (Long, Long) -> Unit,
    onMangaClick: (Long) -> Unit,
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(mangaId) })
) {
    val ui by viewModel.ui.collectAsState()
    val gridState = rememberLazyGridState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (ui.mangaDetail != null) {
                BottomActionBar(
                    isFavorited = ui.isFavorited,
                    readingHistory = ui.readingHistory,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onContinueRead = {
                        val chapter = viewModel.getContinueChapter() ?: viewModel.getLatestChapter()
                        chapter?.let { onChapterClick(mangaId, it.id) }
                    },
                    onStartRead = {
                        viewModel.getLatestChapter()?.let { onChapterClick(mangaId, it.id) }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                ui.isLoading -> LoadingIndicator()
                ui.error != null -> ErrorView(ui.error!!, onRetry = viewModel::refresh)
                else -> ui.mangaDetail?.let { detail ->
                    DetailContent(
                        detail = detail,
                        readCount = ui.readCount,
                        favoriteCount = ui.favoriteCount,
                        isFavorited = ui.isFavorited,
                        recommendations = ui.recommendations,
                        readingHistory = ui.readingHistory,
                        onChapterClick = { onChapterClick(mangaId, it) },
                        onMangaClick = onMangaClick,
                        onBack = onBack,
                        gridState = gridState
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: cn.manxinghai.zhuimange.domain.model.MangaDetail,
    readCount: Int,
    favoriteCount: Int,
    isFavorited: Boolean,
    recommendations: List<Manga>,
    readingHistory: cn.manxinghai.zhuimange.domain.model.ReadingHistory?,
    onChapterClick: (Long) -> Unit,
    onMangaClick: (Long) -> Unit,
    onBack: () -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    var chapterReversed by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 漫画头部信息
        item(span = { GridItemSpan(3) }) {
            DetailHeader(detail, readCount, favoriteCount, onBack)
        }

        // 标签
        if (detail.tags.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                TagRow(tags = detail.tags)
            }
        }

        // 简介可折叠
        item(span = { GridItemSpan(3) }) {
            ExpandableDescription(description = detail.description)
        }

        // 章节标题 + 排序按钮
        item(span = { GridItemSpan(3) }) {
            ChapterSectionHeader(
                chapterCount = detail.chapterCount,
                isReversed = chapterReversed,
                onToggleReverse = { chapterReversed = !chapterReversed }
            )
        }

        // 章节列表（支持排序）
        val sortedChapters = if (chapterReversed) detail.chapters.reversed() else detail.chapters
        items(sortedChapters, key = { it.id }) { chapter ->
            ChapterItem(
                chapter = chapter,
                isRead = readingHistory?.let { history ->
                    chapter.chapterIndex <= detail.chapters.find { it.id == history.lastChapterId }?.chapterIndex ?: 0
                } ?: false,
                onClick = { onChapterClick(chapter.id) }
            )
        }

        // 推荐
        if (recommendations.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                RecommendSection(
                    recommendations = recommendations,
                    onMangaClick = onMangaClick
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    detail: cn.manxinghai.zhuimange.domain.model.MangaDetail,
    readCount: Int,
    favoriteCount: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // 背景封面
        AsyncImage(
            model = detail.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 浮动返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                // 封面
                AsyncImage(
                    model = detail.coverUrl,
                    contentDescription = detail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detail.author.ifBlank { "未知作者" },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip("阅读 $readCount")
                        StatChip("收藏 $favoriteCount")
                        if (detail.status == MangaStatus.FINISHED) {
                            StatChip("已完结", color = Color(0xFF4CAF50))
                        } else if (detail.status == MangaStatus.SERIALIZING) {
                            StatChip("连载中", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color = Color.Gray) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color
    )
}

@Composable
private fun TagRow(tags: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.take(5).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = tag,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandableDescription(description: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        Text(
            text = description.ifBlank { "暂无简介" },
            fontSize = 13.sp,
            color = Color.Gray,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (expanded) "收起" else "展开",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun ChapterSectionHeader(
    chapterCount: Int,
    isReversed: Boolean,
    onToggleReverse: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "章节列表",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "共${chapterCount}话",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.weight(1f))
        // 排序按钮
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggleReverse)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isReversed) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = "排序",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = if (isReversed) "倒序" else "正序",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: Chapter,
    isRead: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.name.ifBlank { "第${chapter.chapterIndex}话" },
                fontSize = 13.sp,
                color = if (isRead) Color.LightGray else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isRead) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已读",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun RecommendSection(
    recommendations: List<Manga>,
    onMangaClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "猜你喜欢",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recommendations.take(3).forEach { manga ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMangaClick(manga.id) }
                ) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = manga.name,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    isFavorited: Boolean,
    readingHistory: cn.manxinghai.zhuimange.domain.model.ReadingHistory?,
    onFavoriteClick: () -> Unit,
    onContinueRead: () -> Unit,
    onStartRead: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 收藏按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorited) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 继续阅读
            if (readingHistory != null) {
                OutlinedButton(
                    onClick = onContinueRead,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "继续 ${readingHistory.lastChapterName}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 开始阅读
            Button(
                onClick = onStartRead,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "开始阅读",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}
