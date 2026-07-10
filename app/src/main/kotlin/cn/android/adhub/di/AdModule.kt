package cn.android.adhub.di

import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.data.local.AdChannelStore
import cn.android.adhub.data.local.TokenStore
import cn.android.adhub.data.provider.baidu.BaiduAdProvider
import cn.android.adhub.data.provider.csj.CsjAdProvider
import cn.android.adhub.data.provider.gdt.GdtAdProvider
import cn.android.adhub.data.provider.umeng.UmengAdProvider
import cn.android.adhub.data.remote.AuthApi
import cn.android.adhub.data.remote.RemoteConfigApi
import cn.android.adhub.data.repository.AdConfigRepositoryImpl
import cn.android.adhub.core.network.AuthInterceptor
import cn.android.adhub.core.network.NetworkClient
import cn.android.adhub.core.network.TokenAuthenticator
import cn.android.adhub.domain.model.AdChannel
import cn.android.adhub.domain.provider.AdProvider
import cn.android.adhub.domain.repository.AdChannelRepository
import cn.android.adhub.domain.repository.AdConfigRepository
import cn.android.adhub.domain.repository.TokenRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
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
    single { cn.android.adhub.core.HotStartInterstitialManager(get(), get(), get()) }
    single { cn.android.adhub.core.PureModeManager(get()) }

    // ── Repository ──
    single<AdConfigRepository> { AdConfigRepositoryImpl(get()) }

    // ── ViewModel ──
    viewModel { cn.android.adhub.ui.screens.banner.BannerViewModel(get(), get(), get()) }
    viewModel { cn.android.adhub.ui.screens.splash.SplashViewModel(get(), get(), get()) }
    viewModel { cn.android.adhub.ui.screens.reward.RewardViewModel(get(), get(), get()) }
    viewModel { cn.android.adhub.ui.screens.interstitial.InterstitialViewModel(get(), get(), get()) }
    viewModel { cn.android.adhub.ui.screens.feed.FeedViewModel(get(), get(), get()) }
}
