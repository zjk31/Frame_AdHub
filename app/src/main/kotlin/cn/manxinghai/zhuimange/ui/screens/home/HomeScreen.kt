package cn.manxinghai.zhuimange.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import cn.manxinghai.zhuimange.domain.model.BannerManga
import cn.manxinghai.zhuimange.domain.model.Category
import cn.manxinghai.zhuimange.domain.model.Manga
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** 首页漫画分类（不含"全部"，"全部"由 MangaCategoryTabs 自动添加） */
private val MANGA_CATEGORIES = listOf("热血", "冒险", "悬疑", "奇幻", "科幻", "古风", "其他", "竞技", "玄幻")

@Composable
fun HomeScreen(
    onMangaClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onMessagesClick: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部标题栏
        HomeTopBar(
            onMessagesClick = onMessagesClick,
            onSearchClick = onSearchClick
        )

        // 分类标签
        MangaCategoryTabs(
            categories = MANGA_CATEGORIES,
            allCategories = ui.categories,
            selectedIndex = ui.selectedCategoryIndex,
            selectedBackendCategoryIndex = ui.selectedBackendCategoryIndex,
            onSelect = viewModel::onCategorySelected,
            onBackendCategorySelect = viewModel::onBackendCategorySelected
        )

        // 主内容
        Box(modifier = Modifier.weight(1f)) {
            when {
                ui.isLoading -> LoadingIndicator()
                ui.error != null -> ErrorView(
                    message = ui.error!!,
                    onRetry = viewModel::refresh
                )
                else -> MangaGridContent(
                    mangaList = ui.mangaList,
                    banners = ui.banners,
                    popularRank = ui.popularRank,
                    collectRank = ui.collectRank,
                    hotRank = ui.hotRank,
                    selectedCategory = ui.selectedCategoryIndex,
                    onMangaClick = onMangaClick,
                    onLoadMore = viewModel::loadMore,
                    isLoadingMore = ui.isLoadingMore
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onMessagesClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题
        Text(
            text = "追漫阁",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        // 右侧：铃铛（消息）
        IconButton(onClick = onMessagesClick) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "消息",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )
        }

        // 右侧：搜索
        IconButton(onClick = onSearchClick) {
            Icon(
                Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun MangaCategoryTabs(
    categories: List<String>,
    allCategories: List<Category>,
    selectedIndex: Int,
    selectedBackendCategoryIndex: Int,
    onSelect: (Int) -> Unit,
    onBackendCategorySelect: (Int) -> Unit
) {
    val displayCategories = listOf("全部") + categories + allCategories.map { it.name }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(displayCategories.size) { index ->
            val name = displayCategories[index]
            val selected = index == selectedIndex
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable { onSelect(index) }
            ) {
                Text(
                    text = name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun MangaGridContent(
    mangaList: List<Manga>,
    banners: List<BannerManga>,
    popularRank: List<Manga>,
    collectRank: List<Manga>,
    hotRank: List<Manga>,
    selectedCategory: Int,
    onMangaClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    // 是否在分类筛选模式（非"全部"）
    val isCategoryFilter = selectedCategory != 0

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (isCategoryFilter) {
            // ── 分类筛选模式：直接显示3列漫画网格（分类标签已表明当前选择） ──
            if (mangaList.isNotEmpty()) {
                mangaChunkedGrid(mangaList, onMangaClick)
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无数据", color = Color.Gray)
                    }
                }
            }
        } else {
            // ── 默认首页模式：Banner + 双卡片 + 全网最火 + 猜你喜欢 ──
            item {
                val displayBanners = if (banners.isNotEmpty()) banners
                else popularRank.take(5).map { BannerManga(it.id, it.name, it.coverUrl) }
                BannerSection(banners = displayBanners, onMangaClick = onMangaClick)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RankCard("人气榜", "人气飙升", Modifier.weight(1f),
                        startColor = Color(0xFFFF6B35), endColor = Color(0xFFFF8C5A)) { /* TODO */ }
                    RankCard("热门榜", "抢先看", Modifier.weight(1f),
                        startColor = Color(0xFF4A90D9), endColor = Color(0xFF6BB5FF)) { /* TODO */ }
                }
            }

            // ── 全网最火（3列） ──
            if (mangaList.isNotEmpty()) {
                item { SectionHeader("全网最火") }
                mangaChunkedGrid(mangaList, onMangaClick)
            }

            // ── 推荐漫画（3列，用热门榜数据） ──
            if (hotRank.isNotEmpty()) {
                item { SectionHeader("推荐漫画") }
                mangaChunkedGrid(hotRank, onMangaClick)
            }

            // ── 猜你喜欢（2列大卡片，用收藏榜数据） ──
            if (collectRank.isNotEmpty()) {
                item { SectionHeader("猜你喜欢") }
                val bigRows = collectRank.chunked(2)
                items(bigRows.size) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bigRows[rowIndex].forEach { manga ->
                            BigMangaCard(manga = manga, onClick = { onMangaClick(manga.id) }, modifier = Modifier.weight(1f))
                        }
                        repeat(2 - bigRows[rowIndex].size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun BannerSection(banners: List<BannerManga>, onMangaClick: (Long) -> Unit) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { banners.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val banner = banners[page]
            Box(Modifier.fillMaxSize().clickable { onMangaClick(banner.id) }) {
                AsyncImage(
                    model = banner.coverUrl, contentDescription = banner.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x80000000)))
                    )
                )
                Text(
                    text = banner.name, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 8.dp else 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

/** 3列漫画网格 — 全网最火/推荐漫画/分类筛选 共用 */
private fun LazyListScope.mangaChunkedGrid(mangaList: List<Manga>, onMangaClick: (Long) -> Unit) {
    val rows = mangaList.chunked(3)
    items(rows.size) { rowIndex ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows[rowIndex].forEach { manga ->
                MangaGridItem(manga = manga, onClick = { onMangaClick(manga.id) }, modifier = Modifier.weight(1f))
            }
            repeat(3 - rows[rowIndex].size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/** 人气榜/热门榜 双卡片 */
@Composable
private fun RankCard(
    title: String, subtitle: String, modifier: Modifier = Modifier,
    startColor: Color = Color.Gray, endColor: Color = Color.Gray,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(startColor, endColor)))) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** 全网最火 3列小卡片 */
@Composable
private fun MangaGridItem(manga: Manga, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(
                model = manga.coverUrl, contentDescription = manga.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            val statusText = when (manga.status) {
                cn.manxinghai.zhuimange.domain.model.MangaStatus.FINISHED -> "已完结"
                else -> "连载中"
            }
            Surface(
                Modifier.align(Alignment.TopEnd).padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (manga.status == cn.manxinghai.zhuimange.domain.model.MangaStatus.FINISHED)
                    Color(0xAA000000) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ) {
                Text(statusText, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(manga.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
        if (manga.tags.isNotEmpty()) {
            Text(manga.tags.take(2).joinToString(" · "), fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text(manga.author.ifBlank { "" }, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 猜你喜欢 2列大卡片 */
@Composable
private fun BigMangaCard(manga: Manga, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(10.dp))) {
            AsyncImage(
                model = manga.coverUrl, contentDescription = manga.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            val statusText = when (manga.status) {
                cn.manxinghai.zhuimange.domain.model.MangaStatus.FINISHED -> "已完结"
                else -> "连载中"
            }
            Surface(
                Modifier.align(Alignment.TopEnd).padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (manga.status == cn.manxinghai.zhuimange.domain.model.MangaStatus.FINISHED)
                    Color(0xAA000000) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ) {
                Text(statusText, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(manga.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
        if (manga.tags.isNotEmpty()) {
            Text(manga.tags.take(3).joinToString(" · "), fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text(manga.author.ifBlank { "" }, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}
