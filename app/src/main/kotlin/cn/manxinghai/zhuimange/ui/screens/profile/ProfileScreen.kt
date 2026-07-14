package cn.manxinghai.zhuimange.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onNavigateToFeedback: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        // 顶部：设置 + 消息图标
        TopActionBar(
            onSettingsClick = onNavigateToSettings,
            onMessagesClick = onNavigateToMessages
        )

        // 用户头像区域（免登录可见，未登录显示"未登录"引导登录）
        val isLoggedIn = ui.user != null
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.uploadAvatar(it) }
        }

        UserInfoSection(
            isLoggedIn = isLoggedIn,
            nickname = ui.user?.nickname ?: "",
            avatar = ui.user?.avatar,
            inviteCode = ui.user?.inviteCode ?: "",
            isUploading = ui.isUploadingAvatar,
            onAvatarClick = { if (isLoggedIn) imagePicker.launch("image/*") },
            onLoginClick = onNavigateToLogin
        )

        Spacer(modifier = Modifier.height(12.dp))

        // VIP 会员卡片
        VipCard()

        Spacer(modifier = Modifier.height(8.dp))

        // 菜单列表
        MenuGroup {
            MenuRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "我的钱包",
                subtitle = "0 元宝",
                onClick = { /* TODO: 钱包页 */ }
            )
            MenuRow(
                icon = Icons.Default.FavoriteBorder,
                title = "我赞过的",
                onClick = { /* TODO: 赞过页 */ }
            )
            MenuRow(
                icon = Icons.Default.Email,
                title = "我的消息",
                badge = null,
                onClick = onNavigateToMessages
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        MenuGroup {
            MenuRow(
                icon = Icons.Default.History,
                title = "阅读历史",
                subtitle = "${ui.historyCount}本",
                onClick = onNavigateToHistory
            )
            MenuRow(
                icon = Icons.Default.Download,
                title = "下载管理",
                subtitle = "${ui.downloadCount}个",
                onClick = onNavigateToDownloads
            )
            MenuRow(
                icon = Icons.Default.Feedback,
                title = "我的反馈",
                onClick = onNavigateToFeedback
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        MenuGroup {
            MenuRow(
                icon = Icons.Default.Share,
                title = "邀请好友",
                subtitle = if (ui.user != null) "邀请码 : ${ui.user!!.inviteCode}" else "",
                onClick = { /* TODO: 分享 */ }
            )
            MenuRow(
                icon = Icons.Default.Settings,
                title = "设置",
                onClick = onNavigateToSettings
            )
        }

        // 退出登录（仅登录后显示）
        if (isLoggedIn) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { viewModel.logout() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "退出登录",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "退出登录",
                        color = Color.Red,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TopActionBar(
    onSettingsClick: () -> Unit,
    onMessagesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "我的",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "消息",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onMessagesClick)
            )
            Icon(
                Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onSettingsClick)
            )
        }
    }
}

@Composable
private fun UserInfoSection(
    isLoggedIn: Boolean,
    nickname: String,
    avatar: String?,
    inviteCode: String,
    isUploading: Boolean = false,
    onAvatarClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 头像 + 昵称行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (isLoggedIn && avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "未登录",
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 昵称 + 登录引导
                Column(modifier = Modifier.weight(1f)) {
                    if (isLoggedIn) {
                        Text(
                            text = nickname.ifBlank { "用户" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "邀请码：$inviteCode",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLoginClick),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "未登录",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "登录后查看更多内容",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计数据行：樱花、关注、粉丝
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "樱花", value = "0")
                StatItem(label = "关注", value = "0")
                StatItem(label = "粉丝", value = "0")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun VipCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2D1B00)
    ) {
        Box {
            // 渐变背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF5C3D00),
                                Color(0xFF8B5E00),
                                Color(0xFFA06900)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "开通会员",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SVIP 畅享全部漫画",
                        fontSize = 12.sp,
                        color = Color(0xAAFFFFFF)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFD700)
                ) {
                    Text(
                        text = "立即开通",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1B00),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(14.dp)
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(start = 50.dp)
    )
}
