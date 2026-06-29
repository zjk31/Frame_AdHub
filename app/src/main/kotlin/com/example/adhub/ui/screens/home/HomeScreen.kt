package com.example.adhub.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBannerClick: () -> Unit,
    onFeedClick: () -> Unit,
    onInterstitialClick: () -> Unit,
    onRewardClick: (slotKey: String) -> Unit,
    onSplashClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frame_AdHub") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdFeatureCard(
                title = "开屏广告",
                subtitle = "Splash Ad",
                icon = Icons.Default.OpenInFull,
                onClick = onSplashClick,
            )
            AdFeatureCard(
                title = "横幅广告",
                subtitle = "Banner Ad",
                icon = Icons.Default.ViewAgenda,
                onClick = onBannerClick,
            )
            AdFeatureCard(
                title = "信息流广告",
                subtitle = "Feed Ad",
                icon = Icons.Default.List,
                onClick = onFeedClick,
            )
            AdFeatureCard(
                title = "插屏广告",
                subtitle = "Interstitial Ad",
                icon = Icons.Default.Screenshot,
                onClick = onInterstitialClick,
            )
            AdFeatureCard(
                title = "激励视频",
                subtitle = "Reward Video",
                icon = Icons.Default.PlayCircle,
                onClick = { onRewardClick("reward") },
            )
            AdFeatureCard(
                title = "任务激励",
                subtitle = "Task Reward",
                icon = Icons.Default.Task,
                onClick = { onRewardClick("taskReward") },
            )
            AdFeatureCard(
                title = "纯净激励",
                subtitle = "Pure Reward",
                icon = Icons.Default.Star,
                onClick = { onRewardClick("pureReward") },
            )
            AdFeatureCard(
                title = "下载配额激励",
                subtitle = "Download Quota Reward",
                icon = Icons.Default.Download,
                onClick = { onRewardClick("downloadQuotaReward") },
            )
        }
    }
}

@Composable
private fun AdFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(icon, contentDescription = null)
            },
        )
    }
}
