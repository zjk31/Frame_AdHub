package com.example.adhub.data

import com.example.adhub.data.remote.RemoteConfigApi
import com.example.adhub.domain.model.AdChannel
import com.example.adhub.domain.provider.AdProvider
import com.example.adhub.domain.repository.AdChannelRepository

/**
 * 广告通道管理器 —— 全局单例，负责：
 * - 本地缓存读写
 * - 远程配置拉取与通道切换
 * - 返回当前通道对应的 [AdProvider]
 */
class AdSdkManager(
    private val channelRepo: AdChannelRepository,
    private val remoteConfigApi: RemoteConfigApi,
    private val providerMap: Map<AdChannel, AdProvider>,
) {
    // ── 当前通道 ──

    val currentChannel: AdChannel get() = channelRepo.cachedChannel

    val currentProvider: AdProvider
        get() = providerMap[currentChannel]
            ?: error("No AdProvider registered for channel $currentChannel")

    // ── 通道切换 ──

    /**
     * 从远程 API 拉取 [AdChannel]，与本地缓存对比；若不同则更新并返回 true。
     * 调用时机：Application.onCreate 或后台定时刷新。
     */
    suspend fun refreshRemoteChannel(): Result<Boolean> {
        return try {
            val response = remoteConfigApi.getGlobalSetting()
            val adType = response.data?.adType ?: return Result.success(false)
            val remoteChannel = AdChannel.fromCode(adType)
            val changed = remoteChannel != channelRepo.cachedChannel
            if (changed) {
                channelRepo.updateChannel(remoteChannel)
            }
            Result.success(changed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── 初始化 ──

    /**
     * 确保当前通道的 SDK 已就绪。若未就绪则调用 [AdProvider.initialize]。
     */
    suspend fun ensureProviderReady(): Result<Unit> {
        val provider = currentProvider
        if (provider.isReady()) return Result.success(Unit)
        return provider.initialize(AppContextHolder.context)
    }
}
