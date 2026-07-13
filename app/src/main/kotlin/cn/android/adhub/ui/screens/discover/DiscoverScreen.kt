package cn.android.adhub.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cn.android.adhub.domain.model.Manga
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(
    onMangaClick: (Long) -> Unit,
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    when {
        ui.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        ui.error != null -> {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = ui.error!!, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::refresh) { Text("重试") }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // 人气榜
                if (ui.popularRank.isNotEmpty()) {
                    item {
                        SectionHeader(title = "人气榜")
                    }
                    item {
                        HorizontalMangaRow(
                            mangaList = ui.popularRank.take(8),
                            onMangaClick = onMangaClick
                        )
                    }
                }

                // 收藏榜
                if (ui.collectRank.isNotEmpty()) {
                    item {
                        SectionHeader(title = "收藏榜")
                    }
                    item {
                        HorizontalMangaRow(
                            mangaList = ui.collectRank.take(8),
                            onMangaClick = onMangaClick
                        )
                    }
                }

                // 热门榜
                if (ui.hotRank.isNotEmpty()) {
                    item {
                        SectionHeader(title = "热门榜")
                    }
                    item {
                        HorizontalMangaRow(
                            mangaList = ui.hotRank.take(8),
                            onMangaClick = onMangaClick
                        )
                    }
                }

                // 推荐列表
                if (ui.recommendations.isNotEmpty()) {
                    item {
                        SectionHeader(title = "猜你喜欢")
                    }
                    items(ui.recommendations.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { manga ->
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
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = manga.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = manga.author.ifBlank { "未知" },
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // 填充空位
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "查看更多 >",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun HorizontalMangaRow(
    mangaList: List<Manga>,
    onMangaClick: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mangaList) { manga ->
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .clickable { onMangaClick(manga.id) }
            ) {
                Box {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    // 排名标
                    val rank = mangaList.indexOf(manga) + 1
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$rank",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = manga.name,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}
