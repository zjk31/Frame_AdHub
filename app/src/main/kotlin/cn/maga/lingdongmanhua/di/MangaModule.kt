package cn.maga.lingdongmanhua.di

import cn.maga.lingdongmanhua.data.api.FileUploadApi
import cn.maga.lingdongmanhua.data.api.ManAppApi
import cn.maga.lingdongmanhua.data.local.ManAppTokenStore
import cn.maga.lingdongmanhua.data.repository.TokenStore
import cn.maga.lingdongmanhua.data.network.ManAppNetworkClient
import cn.maga.lingdongmanhua.data.download.DownloadManager
import cn.maga.lingdongmanhua.data.repository.FileUploadRepository
import cn.maga.lingdongmanhua.data.repository.ManAppRepositoryImpl
import cn.maga.lingdongmanhua.data.repository.UserSession
import cn.maga.lingdongmanhua.data.sse.MessageRepository
import cn.maga.lingdongmanhua.data.settings.GlobalSettingsManager
import cn.maga.lingdongmanhua.domain.repository.ManAppRepository
import cn.maga.lingdongmanhua.ui.screens.auth.AuthViewModel
import cn.maga.lingdongmanhua.ui.screens.bookshelf.BookshelfViewModel
import cn.maga.lingdongmanhua.ui.screens.detail.DetailViewModel
import cn.maga.lingdongmanhua.ui.screens.discover.DiscoverViewModel
import cn.maga.lingdongmanhua.ui.screens.download.DownloadViewModel
import cn.maga.lingdongmanhua.ui.screens.feedback.FeedbackViewModel
import cn.maga.lingdongmanhua.ui.screens.home.HomeViewModel
import cn.maga.lingdongmanhua.ui.screens.message.MessageViewModel
import cn.maga.lingdongmanhua.ui.screens.profile.ProfileViewModel
import cn.maga.lingdongmanhua.ui.screens.reader.ReaderViewModel
import cn.maga.lingdongmanhua.ui.screens.search.SearchViewModel
import cn.maga.lingdongmanhua.ui.screens.settings.SettingsViewModel
import cn.maga.lingdongmanhua.ui.screens.task.TaskViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 漫画 App Koin DI 模块
 * 与广告框架的 AdModule 完全隔离，独立注入
 */
val mangaModule = module {

    // ===================== 数据层 =====================

    /** ManApp Token & User 存储 */
    single<TokenStore> { ManAppTokenStore(androidContext()) }

    single<UserSession> { UserSession(get()) }

    single<ManAppApi> {
        ManAppNetworkClient.getInstance(get()).create(ManAppApi::class.java)
    }

    single<FileUploadApi> {
        ManAppNetworkClient.getInstance(get()).create(FileUploadApi::class.java)
    }

    single<ManAppRepository> { ManAppRepositoryImpl(get(), get()) }

    /** 文件上传 Repository */
    single<FileUploadRepository> { FileUploadRepository(get(), androidContext()) }

    /** 全局设置管理器 */
    single<GlobalSettingsManager> { GlobalSettingsManager(get()) }

    /** 下载管理器 */
    single<DownloadManager> { DownloadManager(androidContext(), get()) }

    /** SSE 消息 Repository */
    single<MessageRepository> {
        MessageRepository(get(), get(), ManAppNetworkClient.getBaseUrl())
    }

    /** APP 版本更新检查器 */
    single<cn.maga.lingdongmanhua.data.update.AppUpdateChecker> {
        cn.maga.lingdongmanhua.data.update.AppUpdateChecker(get(), androidContext())
    }

    // ===================== ViewModels =====================

    viewModel { HomeViewModel(get()) }

    viewModel { params -> DetailViewModel(params.get(), get(), get()) }

    viewModel { params -> ReaderViewModel(params.get(), params.get(), get()) }

    viewModel { SearchViewModel(get()) }

    viewModel { AuthViewModel(get()) }

    viewModel { params -> BookshelfViewModel(params.getOrNull(), get()) }

    viewModel { TaskViewModel(get(), get()) }

    viewModel { FeedbackViewModel(get()) }

    viewModel { ProfileViewModel(get(), get(), get()) }

    viewModel { DownloadViewModel(get(), get(), get()) }

    viewModel { MessageViewModel(get()) }

    viewModel { SettingsViewModel(get(), get(), get(), get()) }

    viewModel { DiscoverViewModel(get()) }
}
