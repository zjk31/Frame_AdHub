package com.example.adhub.di

import com.example.adhub.data.AdSdkManager
import com.example.adhub.data.local.AdChannelStore
import com.example.adhub.data.provider.baidu.BaiduAdProvider
import com.example.adhub.data.provider.csj.CsjAdProvider
import com.example.adhub.data.provider.gdt.GdtAdProvider
import com.example.adhub.data.provider.umeng.UmengAdProvider
import com.example.adhub.data.remote.RemoteConfigApi
import com.example.adhub.data.repository.AdConfigRepositoryImpl
import com.example.adhub.domain.model.AdChannel
import com.example.adhub.domain.provider.AdProvider
import com.example.adhub.domain.repository.AdChannelRepository
import com.example.adhub.domain.repository.AdConfigRepository
import org.koin.dsl.module

val adModule = module {
    // ── 数据源 ──
    single<AdChannelRepository> { AdChannelStore(get()) }
    single<RemoteConfigApi> { com.example.adhub.core.network.NetworkClient.createService() }

    // ── AdProvider 四通道实现 ──
    single<Map<AdChannel, AdProvider>> {
        mapOf(
            AdChannel.Umeng to UmengAdProvider(),
            AdChannel.Csj   to CsjAdProvider(),
            AdChannel.Gdt   to GdtAdProvider(),
            AdChannel.Baidu to BaiduAdProvider(),
        )
    }

    // ── 核心管理器 ──
    single { AdSdkManager(get(), get(), get()) }

    // ── Repository ──
    single<AdConfigRepository> { AdConfigRepositoryImpl(get()) }

    // ── ViewModel ──
    viewModel { com.example.adhub.ui.screens.banner.BannerViewModel(get(), get()) }
    viewModel { com.example.adhub.ui.screens.splash.SplashViewModel(get(), get()) }
}
