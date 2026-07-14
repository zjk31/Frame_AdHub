package cn.maga.lingdongmanhua.di

import cn.maga.lingdongmanhua.data.AdSdkManager
import cn.maga.lingdongmanhua.data.local.AdChannelStore
import cn.maga.lingdongmanhua.data.local.TokenStore
import cn.maga.lingdongmanhua.data.provider.baidu.BaiduAdProvider
import cn.maga.lingdongmanhua.data.provider.csj.CsjAdProvider
import cn.maga.lingdongmanhua.data.provider.gdt.GdtAdProvider
import cn.maga.lingdongmanhua.data.provider.umeng.UmengAdProvider
import cn.maga.lingdongmanhua.data.remote.AuthApi
import cn.maga.lingdongmanhua.data.remote.RemoteConfigApi
import cn.maga.lingdongmanhua.data.repository.AdConfigRepositoryImpl
import cn.maga.lingdongmanhua.core.network.AuthInterceptor
import cn.maga.lingdongmanhua.core.network.NetworkClient
import cn.maga.lingdongmanhua.core.network.TokenAuthenticator
import cn.maga.lingdongmanhua.domain.model.AdChannel
import cn.maga.lingdongmanhua.domain.provider.AdProvider
import cn.maga.lingdongmanhua.domain.repository.AdChannelRepository
import cn.maga.lingdongmanhua.domain.repository.AdConfigRepository
import cn.maga.lingdongmanhua.domain.repository.TokenRepository
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
    single { cn.maga.lingdongmanhua.core.HotStartInterstitialManager(get(), get(), get()) }
    single { cn.maga.lingdongmanhua.core.PureModeManager(get()) }

    // ── Repository ──
    single<AdConfigRepository> { AdConfigRepositoryImpl(get(), get()) }

    // ── ViewModel ──
    viewModel { cn.maga.lingdongmanhua.ui.screens.banner.BannerViewModel(get(), get(), get()) }
    viewModel { cn.maga.lingdongmanhua.ui.screens.splash.SplashViewModel(get(), get(), get()) }
    viewModel { cn.maga.lingdongmanhua.ui.screens.reward.RewardViewModel(get(), get(), get()) }
    viewModel { cn.maga.lingdongmanhua.ui.screens.interstitial.InterstitialViewModel(get(), get(), get()) }
    viewModel { cn.maga.lingdongmanhua.ui.screens.feed.FeedViewModel(get(), get(), get()) }
}
