package cn.manxinghai.zhuimange.di

import cn.manxinghai.zhuimange.data.AdSdkManager
import cn.manxinghai.zhuimange.data.local.AdChannelStore
import cn.manxinghai.zhuimange.data.local.TokenStore
import cn.manxinghai.zhuimange.data.provider.baidu.BaiduAdProvider
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdProvider
import cn.manxinghai.zhuimange.data.provider.gdt.GdtAdProvider
import cn.manxinghai.zhuimange.data.provider.umeng.UmengAdProvider
import cn.manxinghai.zhuimange.data.remote.AuthApi
import cn.manxinghai.zhuimange.data.remote.RemoteConfigApi
import cn.manxinghai.zhuimange.data.repository.AdConfigRepositoryImpl
import cn.manxinghai.zhuimange.core.network.AuthInterceptor
import cn.manxinghai.zhuimange.core.network.NetworkClient
import cn.manxinghai.zhuimange.core.network.TokenAuthenticator
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.provider.AdProvider
import cn.manxinghai.zhuimange.domain.repository.AdChannelRepository
import cn.manxinghai.zhuimange.domain.repository.AdConfigRepository
import cn.manxinghai.zhuimange.domain.repository.TokenRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

    // AuthApi 使用独立的 OkHttpClient（不加认证拦截器），避免循环依赖：
    // NetworkClient → TokenAuthenticator → AuthApi → NetworkClient
    single<AuthApi> {
        val authClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        Retrofit.Builder()
            .baseUrl(NetworkClient.BASE_URL)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

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
    single { cn.manxinghai.zhuimange.core.HotStartInterstitialManager(get(), get(), get()) }
    single { cn.manxinghai.zhuimange.core.PureModeManager(get()) }

    // ── Repository ──
    single<AdConfigRepository> { AdConfigRepositoryImpl(get(), get()) }

    // ── ViewModel ──
    viewModel { cn.manxinghai.zhuimange.ui.screens.banner.BannerViewModel(get(), get(), get()) }
    viewModel { cn.manxinghai.zhuimange.ui.screens.splash.SplashViewModel(get(), get(), get()) }
    viewModel { cn.manxinghai.zhuimange.ui.screens.reward.RewardViewModel(get(), get(), get()) }
    viewModel { cn.manxinghai.zhuimange.ui.screens.interstitial.InterstitialViewModel(get(), get(), get()) }
    viewModel { cn.manxinghai.zhuimange.ui.screens.feed.FeedViewModel(get(), get(), get()) }
}
