package cn.maga.lingdongmanhua.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.maga.lingdongmanhua.domain.model.TaskItem
import cn.maga.lingdongmanhua.ui.theme.AdHubTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onAdReward: (String) -> Unit,  // 回调显示激励广告，广告完成后通知
    viewModel: TaskViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 下载配额卡片
        ui.downloadQuota?.let { quota ->
            QuotaCard(quota = quota)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                ui.isLoading -> LoadingIndicator()
                else -> TaskContent(
                    rewardData = ui.rewardData,
                    isSigningIn = ui.isSigningIn,
                    signInSuccess = ui.signInSuccess,
                    onSignIn = viewModel::signIn,
                    onTaskClick = { taskKey ->
                        when (taskKey) {
                            "ad_watch" -> onAdReward("taskReward")
                            "read_chapter" -> { /* 阅读任务自动完成 */ }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuotaCard(quota: cn.maga.lingdongmanhua.domain.model.DownloadQuota) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QuotaItem(
                icon = Icons.Default.Download,
                label = "剩余下载",
                value = "${quota.remaining}",
                color = MaterialTheme.colorScheme.primary
            )
            QuotaItem(
                icon = Icons.Default.CheckCircle,
                label = "已用",
                value = "${quota.used}",
                color = Color(0xFF4CAF50)
            )
            QuotaItem(
                icon = Icons.Default.Star,
                label = "总配额",
                value = "${quota.total}",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun QuotaItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun TaskContent(
    rewardData: cn.maga.lingdongmanhua.domain.model.RewardData?,
    isSigningIn: Boolean,
    signInSuccess: Boolean,
    onSignIn: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 每日签到
        item {
            SignInCard(
                signedInToday = rewardData?.todaySignedIn == true,
                signInDays = rewardData?.signInDays ?: 0,
                isLoading = isSigningIn,
                success = signInSuccess,
                onSignIn = onSignIn
            )
        }

        // 任务列表
        item {
            Text(
                text = "任务广场",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(rewardData?.taskList ?: emptyList()) { task ->
            TaskItemRow(
                task = task,
                onClick = { onTaskClick(task.taskKey) }
            )
        }
    }
}

@Composable
private fun SignInCard(
    signedInToday: Boolean,
    signInDays: Int,
    isLoading: Boolean,
    success: Boolean,
    onSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (signedInToday) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (signedInToday) "已签到" else "每日签到",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "连续签到 $signInDays 天",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (success) {
                    Text(text = "签到成功！获得下载配额", fontSize = 11.sp, color = Color(0xFF4CAF50))
                }
            }
            Button(
                onClick = onSignIn,
                enabled = !signedInToday && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (signedInToday) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (signedInToday) "已签到" else "签到")
                }
            }
        }
    }
}

@Composable
private fun TaskItemRow(task: TaskItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (task.taskKey) {
                    "ad_watch" -> Icons.Default.Tv
                    "read_chapter" -> Icons.Default.MenuBook
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                tint = if (task.completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.taskName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (task.rewardAmount > 0) {
                    Text(
                        text = "奖励 ${task.rewardAmount} 下载配额",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            if (task.completed) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已完成",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("去完成", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
