package cn.maga.lingdongmanhua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cn.maga.lingdongmanhua.ui.screens.banner.BannerScreen
import cn.maga.lingdongmanhua.ui.screens.banner.BannerViewModel
import cn.maga.lingdongmanhua.ui.screens.detail.DetailScreen
import cn.maga.lingdongmanhua.ui.screens.detail.DetailViewModel
import cn.maga.lingdongmanhua.ui.screens.feed.FeedScreen
import cn.maga.lingdongmanhua.ui.screens.feed.FeedViewModel
import cn.maga.lingdongmanhua.ui.screens.home.HomeScreen
import cn.maga.lingdongmanhua.ui.screens.interstitial.InterstitialScreen
import cn.maga.lingdongmanhua.ui.screens.interstitial.InterstitialViewModel
import cn.maga.lingdongmanhua.ui.screens.reader.ReaderScreen
import cn.maga.lingdongmanhua.ui.screens.reader.ReaderViewModel
import cn.maga.lingdongmanhua.ui.screens.reward.RewardScreen
import cn.maga.lingdongmanhua.ui.screens.reward.RewardViewModel
import cn.maga.lingdongmanhua.ui.screens.search.SearchScreen
import cn.maga.lingdongmanhua.ui.screens.search.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 应用路由定义。
 *
 * 冷启动由 [cn.maga.lingdongmanhua.SplashAdActivity] 独立管理（系统启动页 → 广告 → 跳转本 Activity）。
 * Compose 层直接进入首页，不再包含 SPLASH 路由。
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{mangaId}"
    const val READER = "reader/{chapterId}/{initialPage}"
    const val BANNER = "banner"
    const val FEED = "feed"
    const val INTERSTITIAL = "interstitial"
    const val REWARD = "reward/{slotKey}"

    fun detail(mangaId: Long) = "detail/$mangaId"
    fun reader(chapterId: Long, initialPage: Int = 0) = "reader/$chapterId/$initialPage"
    fun reward(slotKey: String) = "reward/$slotKey"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onMangaClick = { mangaId ->
                    navController.navigate(Routes.detail(mangaId))
                },
                onSearchClick = {
                    navController.navigate(Routes.SEARCH)
                }
            )
        }

        composable(Routes.SEARCH) {
            val vm: SearchViewModel = koinViewModel()
            SearchScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onMangaClick = { mangaId ->
                    navController.navigate("detail/$mangaId")
                }
            )
        }

        composable(Routes.DETAIL) { backStack ->
            val mangaId = backStack.arguments?.getString("mangaId")?.toLongOrNull() ?: return@composable
            val vm: DetailViewModel = koinViewModel(parameters = { parametersOf(mangaId) })
            DetailScreen(
                mangaId = mangaId,
                onBack = { navController.popBackStack() },
                onChapterClick = { mId, chId ->
                    navController.navigate(Routes.reader(chId))
                },
                onMangaClick = { mId ->
                    navController.navigate(Routes.detail(mId))
                },
                viewModel = vm
            )
        }

        composable(Routes.READER) { backStack ->
            val chapterId = backStack.arguments?.getString("chapterId")?.toLongOrNull() ?: return@composable
            val initialPage = backStack.arguments?.getString("initialPage")?.toIntOrNull() ?: 0
            val vm: ReaderViewModel = koinViewModel(parameters = { parametersOf(chapterId, initialPage) })
            ReaderScreen(
                chapterId = chapterId,
                initialPage = initialPage,
                onBack = { navController.popBackStack() },
                onChapterChange = { chId, page ->
                    navController.navigate(Routes.reader(chId, page)) {
                        popUpTo(Routes.READER) { inclusive = true }
                    }
                },
                viewModel = vm
            )
        }

        composable(Routes.BANNER) {
            val vm: BannerViewModel = koinViewModel()
            BannerScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.FEED) {
            val vm: FeedViewModel = koinViewModel()
            FeedScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.INTERSTITIAL) {
            val vm: InterstitialViewModel = koinViewModel()
            InterstitialScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.REWARD) { backStack ->
            val slotKey = backStack.arguments?.getString("slotKey") ?: "reward"
            val vm: RewardViewModel = koinViewModel()
            RewardScreen(slotKey = slotKey, viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
