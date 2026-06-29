package com.example.adhub.di

import com.example.adhub.data.local.AdChannelStore
import com.example.adhub.domain.repository.AdChannelRepository
import org.koin.dsl.module

/**
 * Koin 模块：声明所有依赖关系。
 *
 * Data 层和 Repository 实现在此绑定；
 * AdProvider 实现类通过 Koin Annotations ([@Single]) 自动发现。
 */
val adModule = module {
    single<AdChannelRepository> { AdChannelStore(get()) }
}
