package cn.manxinghai.zhuimange.ui.screens.reader

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.manxinghai.zhuimange.core.MangaImageDecryptor
import cn.manxinghai.zhuimange.domain.model.ChapterPage
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

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
                viewModel.toggleAutoScroll()
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier.fillMaxSize().background(background)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                when (event.key.nativeKeyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (ui.currentPage > 0) viewModel.onPageChanged(ui.currentPage - 1)
                        else if (ui.hasPrev) viewModel.goPrevChapter()
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (ui.currentPage < ui.pages.size - 1) viewModel.onPageChanged(ui.currentPage + 1)
                        else if (ui.hasNext) viewModel.goNextChapter()
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
                        onSliderChange = { page -> viewModel.onPageChanged(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderContent(
    pages: List<ChapterPage>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    val scrollState = rememberScrollState()

    // 自动翻页滚动
    LaunchedEffect(currentPage) {
        val pageHeight = scrollState.viewportSize
        if (pageHeight > 0) scrollState.animateScrollTo(pageHeight * currentPage)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
    ) {
        pages.forEachIndexed { index, pageData ->
            Box(modifier = Modifier.fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            ) {
                MangaPageView(imageUrl = pageData.imageUrl)
            }
        }
    }
}

private val imageClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

@Composable
private fun MangaPageView(imageUrl: String) {
    var imgBytes by remember(imageUrl) { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(imageUrl) {
        withContext(Dispatchers.IO) {
            try {
                val resp = imageClient.newCall(Request.Builder().url(imageUrl).build()).execute()
                if (resp.isSuccessful && resp.body != null) {
                    imgBytes = MangaImageDecryptor.restoreIfNeeded(resp.body!!.bytes())
                }
            } catch (_: Exception) { }
        }
    }

    if (imgBytes != null) {
        AsyncImage(
            model = imgBytes,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

// ===== UI 组件（保持不变） =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    chapterName: String, onBack: () -> Unit,
    isNightMode: Boolean, onNightModeToggle: () -> Unit,
    autoScroll: Boolean, onAutoScrollToggle: () -> Unit,
    autoScrollInterval: Long, onAutoScrollIntervalChange: (Long) -> Unit
) {
    var showAutoScrollSettings by remember { mutableStateOf(false) }
    Surface(color = Color.Black.copy(alpha = 0.7f)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White) }
                Text(chapterName, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = onNightModeToggle) { Icon(if (isNightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode, "夜间模式", tint = Color.White) }
                IconButton(onClick = { showAutoScrollSettings = !showAutoScrollSettings }) {
                    Icon(Icons.Filled.PlayArrow, "自动翻页", tint = if (autoScroll) MaterialTheme.colorScheme.primary else Color.White)
                }
            }
            AnimatedVisibility(visible = showAutoScrollSettings) {
                Surface(color = Color.Black.copy(alpha = 0.8f)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("自动翻页", color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = autoScroll, onCheckedChange = { onAutoScrollToggle() })
                        }
                        if (autoScroll) {
                            Text("间隔: ${(autoScrollInterval / 1000)}秒", color = Color.LightGray, fontSize = 12.sp)
                            Slider(
                                value = autoScrollInterval.toFloat(),
                                onValueChange = { onAutoScrollIntervalChange(it.toLong()) },
                                valueRange = 1000f..10000f,
                                steps = 9
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int, totalPages: Int,
    hasPrev: Boolean, hasNext: Boolean,
    onPrev: () -> Unit, onNext: () -> Unit,
    onSliderChange: (Int) -> Unit
) {
    Surface(color = Color.Black.copy(alpha = 0.7f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPrev, enabled = hasPrev) { Text("上一章", color = if (hasPrev) Color.White else Color.Gray) }
                Spacer(Modifier.weight(1f))
                Text("$currentPage / $totalPages", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onNext, enabled = hasNext) { Text("下一章", color = if (hasNext) Color.White else Color.Gray) }
            }
            Slider(
                value = (currentPage - 1).toFloat(),
                onValueChange = { onSliderChange(it.toInt()) },
                valueRange = 0f..maxOf(0f, (totalPages - 1).toFloat()),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun EmptyChapterView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("该章节暂无内容", color = Color.Gray)
    }
}
