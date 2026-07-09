package com.example.adhub.di

import com.example.adhub.data.AdSdkManager
import com.example.adhub.data.local.AdChannelStore
import com.example.adhub.data.local.TokenStore
import com.example.adhub.data.provider.baidu.BaiduAdProvider
import com.example.adhub.data.provider.csj.CsjAdProvider
import com.example.adhub.data.provider.gdt.GdtAdProvider
import com.example.adhub.data.provider.umeng.UmengAdProvider
import com.example.adhub.data.remote.AuthApi
import com.example.adhub.data.remote.RemoteConfigApi
import com.example.adhub.data.repository.AdConfigRepositoryImpl
import com.example.adhub.core.network.AuthInterceptor
import com.example.adhub.core.network.NetworkClient
import com.example.adhub.core.network.TokenAuthenticator
import com.example.adhub.domain.model.AdChannel
import com.example.adhub.domain.provider.AdProvider
import com.example.adhub.domain.repository.AdChannelRepository
import com.example.adhub.domain.repository.AdConfigRepository
import com.example.adhub.domain.repository.TokenRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val adModule = module {
    // ── 本地存储 ──
    single<AdChannelRepository> { AdChannelStore(get()) }
    single<TokenRepository> { TokenStore(get()) }

    // ── 网络层 ──
    single { AuthInterceptor(get()) }
    single { TokenAuthenticator(get(), get()) }
    single { NetworkClient(get(), get()) }

    // ── API 服务 ──
    single<RemoteConfigApi> { get<NetworkClient>().createService() }
    single<AuthApi> { get<NetworkClient>().createService() }

    // ── AdProvider 四通道实现 ──
    single<Map<AdChannel, AdProvider>> {
        mapOf(
            AdChannel.Umeng to UmengAdProvider(),
            AdChannel.Csj   to CsjAdProvider(get()),
            AdChannel.Gdt   to GdtAdProvider(),
            AdChannel.Baidu to BaiduAdProvider(get()),
        )
    }

    // ── 核心管理器 ──
    single { AdSdkManager(get(), get(), get()) }
    single { com.example.adhub.core.HotStartInterstitialManager(get(), get(), get()) }
    single { com.example.adhub.core.PureModeManager(get()) }

    // ── Repository ──
    single<AdConfigRepository> { AdConfigRepositoryImpl(get()) }

    // ── ViewModel ──
    viewModel { com.example.adhub.ui.screens.banner.BannerViewModel(get(), get(), get()) }
    viewModel { com.example.adhub.ui.screens.splash.SplashViewModel(get(), get(), get()) }
    viewModel { com.example.adhub.ui.screens.reward.RewardViewModel(get(), get(), get()) }
    viewModel { com.example.adhub.ui.screens.interstitial.InterstitialViewModel(get(), get(), get()) }
    viewModel { com.example.adhub.ui.screens.feed.FeedViewModel(get(), get(), get()) }
}
