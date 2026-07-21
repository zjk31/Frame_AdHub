package cn.manxinghai.zhuimange.domain.repository

import cn.manxinghai.zhuimange.data.remote.RemoteConfigData
import cn.manxinghai.zhuimange.domain.model.AdChannel
import cn.manxinghai.zhuimange.domain.model.AdPlacement
import kotlinx.coroutines.flow.StateFlow

/**
 * 广告配置仓库接口：远程配置拉取 + 代码位解析。
 */
interface AdConfigRepository {

    /** 配置版本号，每次 [fetchAdConfig] 更新后递增，UI 层可 collect 响应变化。 */
    val configVersion: StateFlow<Int>

    /** 从远程 API 拉取当前应使用的广告通道。 */
    suspend fun fetchRemoteChannel(): Result<AdChannel>

    /** 对齐 flutter_merge：拉取完整广告配置（含各代码位），失败返回 null。 */
    suspend fun fetchAdConfig(): RemoteConfigData?

    /** 查询当前通道下 [placement] 对应的代码位 ID。
     * 优先使用远程下发的代码位，不可用时回退硬编码值。
     * 若服务端关闭了该类广告（总开关或分类开关），返回空字符串。 */
    suspend fun getCodeId(placement: AdPlacement): String

    /** 检查 [placement] 类型广告是否被服务端启用。 */
    fun isAdTypeEnabled(placement: AdPlacement): Boolean

    /** 是否处于纯净模式（隐藏广告）。 */
    suspend fun isPureMode(): Boolean
}
