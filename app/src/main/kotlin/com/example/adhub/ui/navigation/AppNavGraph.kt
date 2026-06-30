package com.example.adhub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.adhub.ui.screens.banner.BannerScreen
import com.example.adhub.ui.screens.home.HomeScreen
import com.example.adhub.ui.screens.splash.SplashScreen
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
            HomeScreen(
                onBannerClick = { navController.navigate(Routes.BANNER) },
                onFeedClick = { navController.navigate(Routes.FEED) },
                onInterstitialClick = { navController.navigate(Routes.INTERSTITIAL) },
                onRewardClick = { slotKey ->
                    navController.navigate(Routes.reward(slotKey))
                },
                onSplashClick = { navController.navigate(Routes.SPLASH) },
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
        composable(Routes.FEED) { }
        composable(Routes.INTERSTITIAL) { }
        composable(Routes.SPLASH) {
            val vm: com.example.adhub.ui.screens.splash.SplashViewModel = koinViewModel()
            SplashScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REWARD) { }
    }
}
