package cn.maga.lingdongmanhua.domain.repository

import cn.maga.lingdongmanhua.domain.model.AdChannel
import kotlinx.coroutines.flow.Flow

/**
 * 广告通道持久化接口。
 */
interface AdChannelRepository {

    /** 当前缓存的通道。 */
    val cachedChannel: AdChannel

    /** 通道变化流（远程下发更新后发射新值）。 */
    val channelFlow: Flow<AdChannel>

    /** 远程下发通道后调用，持久化并通知。 */
    suspend fun updateChannel(channel: AdChannel)
}
