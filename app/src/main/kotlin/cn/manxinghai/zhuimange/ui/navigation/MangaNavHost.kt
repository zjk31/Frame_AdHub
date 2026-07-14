package cn.manxinghai.zhuimange.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.manxinghai.zhuimange.ui.screens.auth.AuthScreen
import cn.manxinghai.zhuimange.ui.screens.bookshelf.BookshelfScreen
import cn.manxinghai.zhuimange.ui.screens.detail.DetailScreen
import cn.manxinghai.zhuimange.ui.screens.discover.DiscoverScreen
import cn.manxinghai.zhuimange.ui.screens.download.DownloadScreen
import cn.manxinghai.zhuimange.ui.screens.feedback.FeedbackScreen
import cn.manxinghai.zhuimange.ui.screens.home.HomeScreen
import cn.manxinghai.zhuimange.ui.screens.message.MessageCenterScreen
import cn.manxinghai.zhuimange.ui.screens.profile.ProfileScreen
import cn.manxinghai.zhuimange.ui.screens.reader.ReaderScreen
import cn.manxinghai.zhuimange.ui.screens.search.SearchScreen
import cn.manxinghai.zhuimange.ui.screens.settings.SettingsScreen
import cn.manxinghai.zhuimange.ui.screens.task.TaskScreen

/**
 * 漫画 App 导航图
 * 与广告框架的 AppNavGraph 完全独立
 */
object MangaNavRoutes {
    const val HOME = "manga_home"
    const val DETAIL = "manga_detail/{mangaId}"
    const val READER = "manga_reader/{chapterId}?page={page}"
    const val SEARCH = "manga_search"
    const val AUTH = "manga_auth"
    const val BOOKSHELF = "manga_bookshelf"
    const val DISCOVER = "manga_discover"
    const val TASK = "manga_task"
    const val FEEDBACK = "manga_feedback"
    const val PROFILE = "manga_profile"
    const val DOWNLOAD = "manga_download"
    const val MESSAGE = "manga_message"
    const val SETTINGS = "manga_settings"

    fun detailRoute(mangaId: Long) = "manga_detail/$mangaId"
    fun readerRoute(chapterId: Long, page: Int = 0) = "manga_reader/$chapterId?page=$page"
}

@Composable
fun MangaNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = MangaNavRoutes.HOME,
    onLoginSuccess: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 首页
        composable(MangaNavRoutes.HOME) {
            HomeScreen(
                onMangaClick = { mangaId ->
                    navController.navigate(MangaNavRoutes.detailRoute(mangaId))
                },
                onSearchClick = {
                    navController.navigate(MangaNavRoutes.SEARCH)
                },
                onMessagesClick = {
                    navController.navigate(MangaNavRoutes.MESSAGE)
                }
            )
        }

        // 漫画详情
        composable(
            route = MangaNavRoutes.DETAIL,
            arguments = listOf(
                navArgument("mangaId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getLong("mangaId") ?: return@composable
            DetailScreen(
                mangaId = mangaId,
                onBack = { navController.popBackStack() },
                onChapterClick = { mId, chapterId ->
                    navController.navigate(MangaNavRoutes.readerRoute(chapterId))
                },
                onMangaClick = { newMangaId ->
                    navController.navigate(MangaNavRoutes.detailRoute(newMangaId)) {
                        popUpTo(MangaNavRoutes.DETAIL) { inclusive = true }
                    }
                }
            )
        }

        // 阅读器
        composable(
            route = MangaNavRoutes.READER,
            arguments = listOf(
                navArgument("chapterId") { type = NavType.LongType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            val initialPage = backStackEntry.arguments?.getInt("page") ?: 0
            ReaderScreen(
                chapterId = chapterId,
                initialPage = initialPage,
                onBack = { navController.popBackStack() },
                onChapterChange = { newChapterId, newPage ->
                    navController.navigate(MangaNavRoutes.readerRoute(newChapterId, newPage)) {
                        popUpTo(MangaNavRoutes.DETAIL) { inclusive = false }
                    }
                }
            )
        }

        // 搜索
        composable(MangaNavRoutes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onMangaClick = { mangaId ->
                    navController.navigate(MangaNavRoutes.detailRoute(mangaId)) {
                        popUpTo(MangaNavRoutes.SEARCH) { inclusive = true }
                    }
                }
            )
        }

        // 登录注册
        composable(MangaNavRoutes.AUTH) {
            AuthScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack()
                    onLoginSuccess()
                }
            )
        }

        // 书架
        composable(MangaNavRoutes.BOOKSHELF) {
            BookshelfScreen(
                onMangaClick = { mangaId ->
                    navController.navigate(MangaNavRoutes.detailRoute(mangaId))
                },
                onChapterClick = { mangaId, chapterId ->
                    navController.navigate(MangaNavRoutes.readerRoute(chapterId))
                }
            )
        }

        // 发现
        composable(MangaNavRoutes.DISCOVER) {
            DiscoverScreen(
                onMangaClick = { mangaId ->
                    navController.navigate(MangaNavRoutes.detailRoute(mangaId))
                }
            )
        }

        // 任务广场（保留路由，底部导航已改为发现）
        composable(MangaNavRoutes.TASK) {
            TaskScreen(
                onAdReward = { rewardType ->
                    // TODO: 触发激励广告，完成后通知 TaskViewModel
                }
            )
        }

        // 意见反馈
        composable(MangaNavRoutes.FEEDBACK) {
            FeedbackScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 个人中心
        composable(MangaNavRoutes.PROFILE) {
            ProfileScreen(
                onNavigateToFeedback = {
                    navController.navigate(MangaNavRoutes.FEEDBACK)
                },
                onNavigateToHistory = {
                    navController.navigate(MangaNavRoutes.BOOKSHELF)
                },
                onNavigateToLogin = {
                    navController.navigate(MangaNavRoutes.AUTH)
                },
                onNavigateToDownloads = {
                    navController.navigate(MangaNavRoutes.DOWNLOAD)
                },
                onNavigateToMessages = {
                    navController.navigate(MangaNavRoutes.MESSAGE)
                },
                onNavigateToSettings = {
                    navController.navigate(MangaNavRoutes.SETTINGS)
                }
            )
        }

        // 下载管理
        composable(MangaNavRoutes.DOWNLOAD) {
            DownloadScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 消息中心
        composable(MangaNavRoutes.MESSAGE) {
            MessageCenterScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(MangaNavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
