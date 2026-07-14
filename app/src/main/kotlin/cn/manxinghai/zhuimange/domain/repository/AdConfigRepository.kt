package cn.manxinghai.zhuimange.domain.repository

import cn.manxinghai.zhuimange.data.remote.RemoteConfigData
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.model.AdPlacement

/**
 * 广告配置仓库接口：远程配置拉取 + 代码位解析。
 */
interface AdConfigRepository {

    /** 从远程 API 拉取当前应使用的广告通道。 */
    suspend fun fetchRemoteChannel(): Result<AdChannel>

    /** 对齐 flutter_merge：拉取完整广告配置（含各代码位），失败返回 null。 */
    suspend fun fetchAdConfig(): RemoteConfigData?

    /** 查询当前通道下 [placement] 对应的代码位 ID。
     * 优先使用远程下发的代码位，不可用时回退硬编码值。 */
    suspend fun getCodeId(placement: AdPlacement): String

    /** 是否处于纯净模式（隐藏广告）。 */
    suspend fun isPureMode(): Boolean
}
