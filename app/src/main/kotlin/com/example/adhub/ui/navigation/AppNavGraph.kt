package com.example.adhub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.adhub.ui.screens.banner.BannerScreen
import com.example.adhub.ui.screens.feed.FeedScreen
import com.example.adhub.ui.screens.home.HomeScreen
import com.example.adhub.ui.screens.interstitial.InterstitialScreen
import com.example.adhub.ui.screens.reward.RewardScreen
import com.example.adhub.ui.screens.splash.SplashScreen
import com.example.adhub.core.PureModeManager
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 应用路由定义。
 */
object Routes {
    const val HOME = "home"
    const val BANNER = "banner"
    const val FEED = "feed"
    const val INTERSTITIAL = "interstitial"
    const val REWARD = "reward/{slotKey}"
    const val SPLASH = "splash"

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
                onSplashClick = { navController.navigate(Routes.SPLASH) },
                pureModeActive = pureModeActive,
                pureModeRemainingMs = pureModeManager.getRemainingTimeMs(),
            )
        }

        composable(Routes.BANNER) {
            val vm: com.example.adhub.ui.screens.banner.BannerViewModel = koinViewModel()
            BannerScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }

        // TODO: 后续 feature 分支实现
        composable(Routes.FEED) {
            val vm: com.example.adhub.ui.screens.feed.FeedViewModel = koinViewModel()
            FeedScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.INTERSTITIAL) {
            val vm: com.example.adhub.ui.screens.interstitial.InterstitialViewModel = koinViewModel()
            InterstitialScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SPLASH) {
            val vm: com.example.adhub.ui.screens.splash.SplashViewModel = koinViewModel()
            SplashScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REWARD) { backStack ->
            val slotKey = backStack.arguments?.getString("slotKey") ?: "reward"
            val vm: com.example.adhub.ui.screens.reward.RewardViewModel = koinViewModel()
            RewardScreen(
                slotKey = slotKey,
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
