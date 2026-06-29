package com.example.admerge.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.admerge.ui.theme.AdMergeTheme

/**
 * 根 Composable。由 [MainActivity] 调用。
 */
@Composable
fun AdMergeApp() {
    AdMergeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // TODO: NavHost 导航图
        }
    }
}
