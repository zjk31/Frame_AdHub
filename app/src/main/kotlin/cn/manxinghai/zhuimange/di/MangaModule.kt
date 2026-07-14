package cn.manxinghai.zhuimange.di

import cn.manxinghai.zhuimange.data.api.FileUploadApi
import cn.manxinghai.zhuimange.data.api.ManAppApi
import cn.manxinghai.zhuimange.data.local.ManAppTokenStore
import cn.manxinghai.zhuimange.data.repository.TokenStore
import cn.manxinghai.zhuimange.data.network.ManAppNetworkClient
import cn.manxinghai.zhuimange.data.download.DownloadManager
import cn.manxinghai.zhuimange.data.repository.FileUploadRepository
import cn.manxinghai.zhuimange.data.repository.ManAppRepositoryImpl
import cn.manxinghai.zhuimange.data.repository.UserSession
import cn.manxinghai.zhuimange.data.sse.MessageRepository
import cn.manxinghai.zhuimange.data.settings.GlobalSettingsManager
import cn.manxinghai.zhuimange.domain.repository.ManAppRepository
import cn.manxinghai.zhuimange.ui.screens.auth.AuthViewModel
import cn.manxinghai.zhuimange.ui.screens.bookshelf.BookshelfViewModel
import cn.manxinghai.zhuimange.ui.screens.detail.DetailViewModel
import cn.manxinghai.zhuimange.ui.screens.discover.DiscoverViewModel
import cn.manxinghai.zhuimange.ui.screens.download.DownloadViewModel
import cn.manxinghai.zhuimange.ui.screens.feedback.FeedbackViewModel
import cn.manxinghai.zhuimange.ui.screens.home.HomeViewModel
import cn.manxinghai.zhuimange.ui.screens.message.MessageViewModel
import cn.manxinghai.zhuimange.ui.screens.profile.ProfileViewModel
import cn.manxinghai.zhuimange.ui.screens.reader.ReaderViewModel
import cn.manxinghai.zhuimange.ui.screens.search.SearchViewModel
import cn.manxinghai.zhuimange.ui.screens.settings.SettingsViewModel
import cn.manxinghai.zhuimange.ui.screens.task.TaskViewModel
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
    single<cn.manxinghai.zhuimange.data.update.AppUpdateChecker> {
        cn.manxinghai.zhuimange.data.update.AppUpdateChecker(get(), androidContext())
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
