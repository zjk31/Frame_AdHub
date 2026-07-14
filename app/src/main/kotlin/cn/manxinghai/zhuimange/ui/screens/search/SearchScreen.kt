package cn.manxinghai.zhuimange.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.manxinghai.zhuimange.domain.model.Manga
import cn.manxinghai.zhuimange.domain.model.SearchSuggestion
import cn.manxinghai.zhuimange.ui.theme.AdHubTheme
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onMangaClick: (Long) -> Unit,
    viewModel: SearchViewModel = koinViewModel<SearchViewModel>()
) {
    val ui by viewModel.ui.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // 自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 触底加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 3
        }.collect { shouldLoad ->
            if (shouldLoad && ui.results.isNotEmpty()) {
                viewModel.loadMore()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            }

            OutlinedTextField(
                value = ui.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("搜索漫画名称/作者", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.onSearch()
                        focusManager.clearFocus()
                    }
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                trailingIcon = {
                    if (ui.query.isNotBlank()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Default.Clear, contentDescription = "清除", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(
                onClick = {
                    viewModel.onSearch()
                    focusManager.clearFocus()
                }
            ) {
                Text("搜索", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
        }

        // 内容区
        Box(modifier = Modifier.weight(1f)) {
            when {
                ui.isSearching && ui.results.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                // 搜索联想
                ui.suggestions.isNotEmpty() -> {
                    SuggestionList(
                        suggestions = ui.suggestions,
                        onSelect = { suggestion ->
                            onMangaClick(suggestion.id)
                        }
                    )
                }
                // 搜索结果
                ui.results.isNotEmpty() -> {
                    SearchResultList(
                        results = ui.results,
                        onMangaClick = onMangaClick,
                        listState = listState,
                        isLoadingMore = ui.isSearching
                    )
                }
                // 空状态
                ui.hasSearched && ui.results.isEmpty() -> {
                    EmptySearchResult(query = ui.query)
                }
                // 默认提示
                else -> {
                    SearchHint(
                        onHotSearch = { keyword ->
                            viewModel.onQueryChange(keyword)
                            viewModel.onSearch(keyword)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<SearchSuggestion>,
    onSelect: (SearchSuggestion) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(suggestions) { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(suggestion) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = suggestion.author.ifBlank { "未知作者" },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                AsyncImage(
                    model = suggestion.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(36.dp)
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun SearchResultList(
    results: List<Manga>,
    onMangaClick: (Long) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isLoadingMore: Boolean
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(results, key = { it.id }) { manga ->
            SearchResultItem(
                manga = manga,
                onClick = { onMangaClick(manga.id) }
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(manga: Manga, onClick: () -> Unit) {
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
            model = manga.coverUrl,
            contentDescription = manga.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(70.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = manga.author.ifBlank { "未知作者" },
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                manga.tags.take(2).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "未找到「$query」相关漫画",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "试试其他关键词，或反馈给我们",
            fontSize = 12.sp,
            color = Color.LightGray
        )
    }
}

@Composable
private fun SearchHint(
    onHotSearch: (String) -> Unit = {}
) {
    val hotKeywords = listOf("热血", "恋爱", "搞笑", "冒险", "玄幻", "日常", "悬疑", "动作")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "热门搜索",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hotKeywords.forEach { keyword ->
                Surface(
                    modifier = Modifier.clickable { onHotSearch(keyword) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = keyword,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
