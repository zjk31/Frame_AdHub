package cn.manxinghai.zhuimange.data

import cn.manxinghai.zhuimange.core.AppContextHolder
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.provider.AdProvider
import cn.manxinghai.zhuimange.domain.repository.AdChannelRepository

/**
 * 广告通道管理器。
 *
 * 从本地缓存读通道，子 App 通过写入 SharedPreferences 切换通道。
 * 不包含远程 API 调用——子 App 自己负责从后端拉取配置并写入缓存。
 */
class AdSdkManager(
    private val channelRepo: AdChannelRepository,
    private val providerMap: Map<AdChannel, AdProvider>,
) {

    val currentChannel: AdChannel get() = channelRepo.cachedChannel

    val currentProvider: AdProvider
        get() = providerMap[currentChannel]
            ?: error("No AdProvider registered for channel $currentChannel")

    /**
     * 从本地缓存刷新通道（子 App 写入 prefs 后调用）。
     */
    suspend fun refreshLocalChannel(): Result<AdChannel> {
        return try {
            val channel = channelRepo.cachedChannel
            Result.success(channel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据 adType 切换通道。
     */
    suspend fun applyRemoteAdType(rawAdType: Int) {
        val remoteChannel = AdChannel.fromCode(rawAdType)
        val current = channelRepo.cachedChannel
        if (remoteChannel != current) {
            channelRepo.updateChannel(remoteChannel)
        }
    }

    /**
     * 手动切换到指定通道（测试/设置页面）。
     */
    suspend fun switchChannel(channel: AdChannel) {
        channelRepo.updateChannel(channel)
    }

    /**
     * 确保当前通道的 SDK 已就绪。
     */
    suspend fun ensureProviderReady(): Result<Unit> {
        val provider = currentProvider
        if (provider.isReady()) return Result.success(Unit)
        return provider.initialize(AppContextHolder.context)
    }
}
