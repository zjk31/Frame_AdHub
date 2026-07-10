package cn.android.adhub.domain.repository

import cn.android.adhub.domain.model.AdChannel
import cn.android.adhub.domain.model.AdPlacement

/**
 * 广告配置仓库接口：远程配置拉取 + 代码位解析。
 */
interface AdConfigRepository {

    /** 从远程 API 拉取当前应使用的广告通道。 */
    suspend fun fetchRemoteChannel(): Result<AdChannel>

    /** 查询当前通道下 [placement] 对应的代码位 ID。 */
    suspend fun getCodeId(placement: AdPlacement): String

    /** 是否处于纯净模式（隐藏广告）。 */
    suspend fun isPureMode(): Boolean
}
