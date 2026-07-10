package cn.android.adhub.data.repository

import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.domain.model.AdChannel
import cn.android.adhub.domain.model.AdPlacement
import cn.android.adhub.domain.repository.AdConfigRepository

/**
 * [AdConfigRepository] 实现：通过 [AdSdkManager] 获取当前通道 + 对应代码位。
 */
class AdConfigRepositoryImpl(
    private val adSdkManager: AdSdkManager,
) : AdConfigRepository {

    override suspend fun fetchRemoteChannel(): Result<AdChannel> {
        return adSdkManager.refreshRemoteChannel().map { adSdkManager.currentChannel }
    }

    override suspend fun getCodeId(placement: AdPlacement): String {
        val channel = adSdkManager.currentChannel
        return AdCodeIdResolver.resolve(channel, placement)
    }

    override suspend fun isPureMode(): Boolean {
        // TODO: 调用 /reward/isPureTaskCompleted 接口
        return false
    }
}
