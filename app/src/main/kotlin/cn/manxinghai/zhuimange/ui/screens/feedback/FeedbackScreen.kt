package cn.manxinghai.zhuimange.ui.screens.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.manxinghai.zhuimange.ui.theme.AdHubTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.submitSuccess) {
        if (ui.submitSuccess) {
            // 显示成功提示后返回
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("意见反馈", fontWeight = FontWeight.Medium) },
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
        if (ui.submitSuccess) {
            SuccessView(onBack = onBack, onSubmitAnother = viewModel::reset)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 反馈类型选择
                Text(
                    text = "反馈类型",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeedbackType.entries.forEach { type ->
                        FilterChip(
                            selected = ui.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            label = { Text(type.label, fontSize = 12.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // 类型化表单
                when (ui.type) {
                    FeedbackType.SYSTEM -> SystemFeedbackForm(
                        message = ui.message,
                        onMessageChange = viewModel::onMessageChange
                    )
                    FeedbackType.FIND_BOOK -> FindBookForm(
                        title = ui.bookTitle,
                        keyword = ui.bookKeyword,
                        onTitleChange = viewModel::onBookTitleChange,
                        onKeywordChange = viewModel::onBookKeywordChange
                    )
                    FeedbackType.BOOK_ERROR -> BookErrorForm(
                        errorType = ui.bookErrorType,
                        message = ui.message,
                        onErrorTypeChange = viewModel::onBookErrorTypeChange,
                        onMessageChange = viewModel::onMessageChange
                    )
                }

                // 错误提示
                if (ui.error != null) {
                    Text(
                        text = ui.error!!,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 提交按钮
                Button(
                    onClick = viewModel::submit,
                    enabled = !ui.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (ui.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("提交反馈", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemFeedbackForm(
    message: String,
    onMessageChange: (String) -> Unit
) {
    Column {
        Text(
            text = "问题描述",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            placeholder = { Text("请详细描述您遇到的问题...", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun FindBookForm(
    title: String,
    keyword: String,
    onTitleChange: (String) -> Unit,
    onKeywordChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = "漫画名称 *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("请输入漫画名称", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
        Column {
            Text(
                text = "补充关键词（可选）",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                placeholder = { Text("作者、题材等关键词", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun BookErrorForm(
    errorType: String,
    message: String,
    onErrorTypeChange: (String) -> Unit,
    onMessageChange: (String) -> Unit
) {
    val errorTypes = listOf("内容错误", "图片缺失", "翻译错误", "章节错乱", "其他")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "错误类型 *",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            errorTypes.forEach { type ->
                FilterChip(
                    selected = errorType == type,
                    onClick = { onErrorTypeChange(type) },
                    label = { Text(type, fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }
        Column {
            Text(
                text = "错误描述 *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("请描述具体错误内容...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun SuccessView(onBack: () -> Unit, onSubmitAnother: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "反馈已提交",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "感谢您的反馈，我们会尽快处理",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("返回")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSubmitAnother) {
            Text("继续反馈", color = MaterialTheme.colorScheme.primary)
        }
    }
}
