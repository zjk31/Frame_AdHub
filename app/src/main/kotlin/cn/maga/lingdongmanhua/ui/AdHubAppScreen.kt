package cn.maga.lingdongmanhua.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.maga.lingdongmanhua.ui.components.AppUpdateDialog
import cn.maga.lingdongmanhua.ui.navigation.MangaNavHost
import cn.maga.lingdongmanhua.ui.navigation.MangaNavRoutes
import cn.maga.lingdongmanhua.ui.theme.AdHubTheme
import org.koin.compose.koinInject

@Composable
fun AdHubApp() {
    AdHubTheme {
        MangaAppContent()
    }
}

private enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(MangaNavRoutes.HOME, Icons.Default.Home, "首页"),
    BOOKSHELF(MangaNavRoutes.BOOKSHELF, Icons.Default.Bookmarks, "书架"),
    DISCOVER(MangaNavRoutes.DISCOVER, Icons.Default.Explore, "发现"),
    PROFILE(MangaNavRoutes.PROFILE, Icons.Default.Person, "我的"),
}

@Composable
private fun MangaAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val appUpdateChecker: cn.maga.lingdongmanhua.data.update.AppUpdateChecker = koinInject()
    val updateState by appUpdateChecker.updateState.collectAsState()

    val showBottomBar = currentDestination?.route?.let { route ->
        route in listOf(
            MangaNavRoutes.HOME,
            MangaNavRoutes.BOOKSHELF,
            MangaNavRoutes.DISCOVER,
            MangaNavRoutes.PROFILE
        )
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    BottomNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, softWrap = false) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MangaNavHost(
                navController = navController,
                onLoginSuccess = {}
            )

            AppUpdateDialog(
                updateState = updateState,
                onDismiss = { appUpdateChecker.reset() },
                onDownload = { url ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    navController.context.startActivity(intent)
                }
            )
        }
    }
}
