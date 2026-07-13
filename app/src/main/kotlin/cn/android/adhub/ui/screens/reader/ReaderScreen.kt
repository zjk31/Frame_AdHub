package cn.android.adhub.ui.screens.reader

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.android.adhub.domain.model.ChapterPage
import cn.android.adhub.ui.theme.AdHubTheme
import kotlinx.coroutines.delay
import org.koin.core.parameter.parametersOf
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: Long,
    initialPage: Int,
    onBack: () -> Unit,
    onChapterChange: (Long, Int) -> Unit,
    viewModel: ReaderViewModel = koinViewModel(parameters = { parametersOf(chapterId, initialPage) })
) {
    val ui by viewModel.ui.collectAsState()
    val background = if (ui.isNightMode) Color.Black else Color(0xFF1A1A1A)
    val focusRequester = remember { FocusRequester() }

    // 自动翻页
    LaunchedEffect(ui.autoScroll, ui.currentPage, ui.pages.size) {
        if (ui.autoScroll && ui.pages.isNotEmpty()) {
            delay(ui.autoScrollInterval)
            if (ui.currentPage < ui.pages.size - 1) {
                viewModel.onPageChanged(ui.currentPage + 1)
            } else if (ui.hasNext) {
                viewModel.goNextChapter()
            } else {
                viewModel.toggleAutoScroll() // 到末尾自动停止
            }
        }
    }

    // 焦点请求（用于音量键）
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                when (event.key.nativeKeyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (ui.currentPage > 0) {
                            viewModel.onPageChanged(ui.currentPage - 1)
                        } else if (ui.hasPrev) {
                            viewModel.goPrevChapter()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (ui.currentPage < ui.pages.size - 1) {
                            viewModel.onPageChanged(ui.currentPage + 1)
                        } else if (ui.hasNext) {
                            viewModel.goNextChapter()
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        when {
            ui.isLoading -> LoadingIndicator()
            ui.error != null -> ErrorView(ui.error!!, onRetry = viewModel::retry)
            ui.pages.isEmpty() -> EmptyChapterView()
            else -> {
                ReaderContent(
                    pages = ui.pages,
                    currentPage = ui.currentPage,
                    onPageChanged = viewModel::onPageChanged,
                    onTap = viewModel::toggleControls
                )

                // 顶部控制栏
                AnimatedVisibility(
                    visible = ui.showControls,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ReaderTopBar(
                        chapterName = ui.chapter?.name ?: "",
                        onBack = onBack,
                        isNightMode = ui.isNightMode,
                        onNightModeToggle = viewModel::toggleNightMode,
                        autoScroll = ui.autoScroll,
                        onAutoScrollToggle = viewModel::toggleAutoScroll,
                        autoScrollInterval = ui.autoScrollInterval,
                        onAutoScrollIntervalChange = viewModel::setAutoScrollInterval
                    )
                }

                // 底部控制栏
                AnimatedVisibility(
                    visible = ui.showControls,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomBar(
                        currentPage = ui.currentPage + 1,
                        totalPages = ui.totalPages,
                        hasPrev = ui.hasPrev,
                        hasNext = ui.hasNext,
                        onPrev = viewModel::goPrevChapter,
                        onNext = viewModel::goNextChapter,
                        onSliderChange = { page ->
                            viewModel.onPageChanged(page)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 垂直翻页阅读器
 * 滑动模式：VerticalPager
 */
@Composable
private fun ReaderContent(
    pages: List<ChapterPage>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { pages.size }
    )

    // 同步 pager → ViewModel
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val pageData = pages[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() }
                    )
                }
        ) {
            AsyncImage(
                model = pageData.imageUrl,
                contentDescription = "第${page + 1}页",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            // 加载进度
            if (pageData.imageUrl.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    chapterName: String,
    onBack: () -> Unit,
    isNightMode: Boolean,
    onNightModeToggle: () -> Unit,
    autoScroll: Boolean = false,
    onAutoScrollToggle: () -> Unit = {},
    autoScrollInterval: Long = 3000L,
    onAutoScrollIntervalChange: (Long) -> Unit = {}
) {
    var showAutoScrollSettings by remember { mutableStateOf(false) }

    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Text(
                    text = chapterName,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                // 自动翻页
                IconButton(onClick = { showAutoScrollSettings = !showAutoScrollSettings }) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "自动翻页",
                        tint = if (autoScroll) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                // 夜间模式
                IconButton(onClick = onNightModeToggle) {
                    Icon(
                        imageVector = if (isNightMode) Icons.Default.Brightness5 else Icons.Default.Brightness2,
                        contentDescription = "夜间模式",
                        tint = Color.White
                    )
                }
            }

            // 自动翻页设置面板
            AnimatedVisibility(visible = showAutoScrollSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动翻页", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { onAutoScrollToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (autoScroll) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("间隔: ${(autoScrollInterval / 1000)}秒", color = Color.LightGray, fontSize = 12.sp)
                        Slider(
                            value = autoScrollInterval.toFloat(),
                            onValueChange = { onAutoScrollIntervalChange(it.toLong()) },
                            valueRange = 1000f..10000f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSliderChange: (Int) -> Unit = {}
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 进度滑块
            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Slider(
                        value = (currentPage - 1).toFloat(),
                        onValueChange = { onSliderChange(it.toInt()) },
                        valueRange = 0f..(totalPages - 1).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "$totalPages",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // 章节导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrev,
                    enabled = hasPrev,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Text("上一章", fontSize = 13.sp)
                }

                Text(
                    text = "$currentPage / $totalPages",
                    color = Color.White,
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = onNext,
                    enabled = hasNext,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    Text("下一章", fontSize = 13.sp)
                }
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
        CircularProgressIndicator(color = Color.White)
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
        Button(onClick = onRetry) { Text("重试", color = Color.White) }
    }
}

@Composable
private fun EmptyChapterView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("该章节暂无内容", color = Color.Gray)
    }
}
