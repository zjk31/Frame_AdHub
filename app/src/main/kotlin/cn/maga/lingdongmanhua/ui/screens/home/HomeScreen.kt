package cn.maga.lingdongmanhua.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import cn.maga.lingdongmanhua.domain.model.BannerManga
import cn.maga.lingdongmanhua.domain.model.Category
import cn.maga.lingdongmanhua.domain.model.Manga
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** 首页漫画分类（替代性别筛选） */
private val MANGA_CATEGORIES = listOf(
    "热血", "冒险", "悬疑", "奇幻", "科幻",
    "古风", "其他", "竞技", "玄幻"
)

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
            text = "灵动漫画",
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
    onMangaClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 6
        }.collect { shouldLoad ->
            if (shouldLoad && mangaList.isNotEmpty()) {
                onLoadMore()
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(3) }) {
            val displayBanners = if (banners.isNotEmpty()) banners
            else popularRank.take(5).map { BannerManga(it.id, it.name, it.coverUrl) }
            BannerSection(banners = displayBanners, onMangaClick = onMangaClick)
        }

        if (popularRank.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                RankSection(title = "人气榜", rankList = popularRank, onMangaClick = onMangaClick)
            }
        }

        item(span = { GridItemSpan(3) }) {
            Text(
                text = "热门推荐",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        items(mangaList, key = { it.id }) { manga ->
            MangaGridItem(manga = manga, onClick = { onMangaClick(manga.id) })
        }

        if (isLoadingMore) {
            item(span = { GridItemSpan(3) }) {
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

@Composable
private fun RankSection(title: String, rankList: List<Manga>, onMangaClick: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Text("查看更多 >", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rankList.take(6)) { manga ->
                Column(Modifier.width(80.dp).clickable { onMangaClick(manga.id) }) {
                    Box {
                        AsyncImage(
                            model = manga.coverUrl, contentDescription = manga.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(6.dp))
                        )
                        Surface(
                            Modifier.align(Alignment.TopStart).padding(4.dp),
                            shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "${rankList.indexOf(manga) + 1}", color = Color.White,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(manga.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
private fun MangaGridItem(manga: Manga, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(model = manga.coverUrl, contentDescription = manga.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (manga.status == cn.maga.lingdongmanhua.domain.model.MangaStatus.FINISHED) {
                Surface(Modifier.align(Alignment.TopEnd).padding(4.dp), shape = RoundedCornerShape(4.dp), color = Color(0xAA000000)) {
                    Text("完结", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            Surface(Modifier.align(Alignment.BottomStart).padding(4.dp), shape = RoundedCornerShape(4.dp), color = Color(0xAA000000)) {
                Text("${manga.chapterCount}话", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(manga.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
        Text(manga.author.ifBlank { "未知作者" }, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
