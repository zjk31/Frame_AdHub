package cn.android.adhub.data.repository

import cn.android.adhub.data.api.BaiduOcpcApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * OCPC 回调 Repository
 * 用于百度广告转化追踪
 */
class OcpcRepository(
    private val api: BaiduOcpcApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 上报激活
     */
    fun reportActivate(clickId: String) {
        scope.launch {
            runCatching {
                api.baiduOcpcCallback(clickId, "activate")
            }
        }
    }

    /**
     * 上报注册
     */
    fun reportRegister(clickId: String) {
        scope.launch {
            runCatching {
                api.baiduOcpcCallback(clickId, "register")
            }
        }
    }

    /**
     * 上报自定义行动
     */
    fun reportAction(clickId: String, actionType: String, value: String? = null) {
        scope.launch {
            runCatching {
                api.baiduOcpcCallback(clickId, actionType, value)
            }
        }
    }
}
