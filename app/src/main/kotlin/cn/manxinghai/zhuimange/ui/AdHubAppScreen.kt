package cn.manxinghai.zhuimange.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import cn.manxinghai.zhuimange.ui.navigation.AppNavGraph
import cn.manxinghai.zhuimange.ui.theme.AdHubTheme

/**
 * 根 Composable，由 [MainActivity] 调用。
 */
@Composable
fun AdHubApp() {
    AdHubTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }
    }
}
