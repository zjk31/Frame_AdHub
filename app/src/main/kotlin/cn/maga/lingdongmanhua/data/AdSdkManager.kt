package cn.maga.lingdongmanhua.data

import cn.maga.lingdongmanhua.core.AppContextHolder
import cn.maga.lingdongmanhua.data.remote.RemoteConfigApi
import cn.maga.lingdongmanhua.domain.model.AdChannel
import cn.maga.lingdongmanhua.domain.provider.AdProvider
import cn.maga.lingdongmanhua.domain.repository.AdChannelRepository

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

    // ── 通道切换（带同步锁防止竞态） ──

    private val lock = Any()

    /**
     * 从远程 API 拉取 [AdChannel]，与本地缓存对比；若不同则更新并返回 true。
     * 调用时机：Application.onCreate 或后台定时刷新。
     */
    suspend fun refreshRemoteChannel(): Result<Boolean> {
        return try {
            val response = remoteConfigApi.getGlobalSetting()
            val adType = response.data?.adType ?: return Result.success(false)
            val remoteChannel = AdChannel.fromCode(adType)

            // 比较-写入操作在锁内保证原子性
            val changed = synchronized(lock) {
                val current = channelRepo.cachedChannel
                val isChanged = remoteChannel != current
                isChanged
            }
            if (changed) {
                channelRepo.updateChannel(remoteChannel)
            }

            Result.success(changed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── 通道切换（按需） ──

    /**
     * 根据服务端下发的 adType 切换通道（对齐 flutter_merge 的 cacheRemoteAdType）。
     * 由 [AdConfigRepository.fetchAdConfig] 调用，确保广告通道与服务端一致。
     */
    suspend fun applyRemoteAdType(rawAdType: Int) {
        val remoteChannel = AdChannel.fromCode(rawAdType)
        val current = channelRepo.cachedChannel
        if (remoteChannel != current) {
            channelRepo.updateChannel(remoteChannel)
        }
    }

    /**
     * 手动切换到指定通道（不依赖远程配置）。
     * 用于测试页面或本地设置。
     */
    suspend fun switchChannel(channel: AdChannel) {
        synchronized(lock) { /* 获取锁确保与 refreshRemoteChannel 的并发安全 */ }
        channelRepo.updateChannel(channel)
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
