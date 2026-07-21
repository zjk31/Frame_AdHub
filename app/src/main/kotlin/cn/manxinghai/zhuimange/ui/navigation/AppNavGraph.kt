package cn.manxinghai.zhuimange.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cn.manxinghai.zhuimange.ui.screens.banner.BannerScreen
import cn.manxinghai.zhuimange.ui.screens.banner.BannerViewModel
import cn.manxinghai.zhuimange.ui.screens.feed.FeedScreen
import cn.manxinghai.zhuimange.ui.screens.feed.FeedViewModel
import cn.manxinghai.zhuimange.ui.screens.home.HomeScreen
import cn.manxinghai.zhuimange.ui.screens.interstitial.InterstitialScreen
import cn.manxinghai.zhuimange.ui.screens.interstitial.InterstitialViewModel
import cn.manxinghai.zhuimange.ui.screens.reward.RewardScreen
import cn.manxinghai.zhuimange.ui.screens.reward.RewardViewModel
import cn.manxinghai.zhuimange.core.PureModeManager
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 应用路由定义。
 *
 * 冷启动由 [cn.manxinghai.zhuimange.SplashAdActivity] 独立管理（系统启动页 → 广告 → 跳转本 Activity）。
 * Compose 层直接进入首页，不再包含 SPLASH 路由。
 */
object Routes {
    const val HOME = "home"
    const val BANNER = "banner"
    const val FEED = "feed"
    const val INTERSTITIAL = "interstitial"
    const val REWARD = "reward/{slotKey}"

    fun reward(slotKey: String) = "reward/$slotKey"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val pureModeManager: PureModeManager = koinInject()
            val pureModeActive by pureModeManager.isPureModeActive.collectAsState()
            HomeScreen(
                onBannerClick = { navController.navigate(Routes.BANNER) },
                onFeedClick = { navController.navigate(Routes.FEED) },
                onInterstitialClick = { navController.navigate(Routes.INTERSTITIAL) },
                onRewardClick = { slotKey ->
                    navController.navigate(Routes.reward(slotKey))
                },
                pureModeActive = pureModeActive,
                pureModeRemainingMs = pureModeManager.getRemainingTimeMs(),
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
