package cn.android.adhub.di

import cn.android.adhub.data.api.FileUploadApi
import cn.android.adhub.data.api.ManAppApi
import cn.android.adhub.data.local.ManAppTokenStore
import cn.android.adhub.data.repository.TokenStore
import cn.android.adhub.data.network.ManAppNetworkClient
import cn.android.adhub.data.download.DownloadManager
import cn.android.adhub.data.repository.FileUploadRepository
import cn.android.adhub.data.repository.ManAppRepositoryImpl
import cn.android.adhub.data.repository.UserSession
import cn.android.adhub.data.sse.MessageRepository
import cn.android.adhub.data.settings.GlobalSettingsManager
import cn.android.adhub.domain.repository.ManAppRepository
import cn.android.adhub.ui.screens.auth.AuthViewModel
import cn.android.adhub.ui.screens.bookshelf.BookshelfViewModel
import cn.android.adhub.ui.screens.detail.DetailViewModel
import cn.android.adhub.ui.screens.discover.DiscoverViewModel
import cn.android.adhub.ui.screens.download.DownloadViewModel
import cn.android.adhub.ui.screens.feedback.FeedbackViewModel
import cn.android.adhub.ui.screens.home.HomeViewModel
import cn.android.adhub.ui.screens.message.MessageViewModel
import cn.android.adhub.ui.screens.profile.ProfileViewModel
import cn.android.adhub.ui.screens.reader.ReaderViewModel
import cn.android.adhub.ui.screens.search.SearchViewModel
import cn.android.adhub.ui.screens.settings.SettingsViewModel
import cn.android.adhub.ui.screens.task.TaskViewModel
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
    single<cn.android.adhub.data.update.AppUpdateChecker> {
        cn.android.adhub.data.update.AppUpdateChecker(get(), androidContext())
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
