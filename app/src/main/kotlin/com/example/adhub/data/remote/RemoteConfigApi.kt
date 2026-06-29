package com.example.adhub.data.remote

import com.example.adhub.domain.model.AdChannel
import retrofit2.http.GET

/**
 * 远程配置 API。
 *
 * 返回示例: { "code": 0, "data": { "adType": 1 }, "message": "ok" }
 */
interface RemoteConfigApi {

    @GET("globalSetting/get")
    suspend fun getGlobalSetting(): RemoteConfigResponse
}

data class RemoteConfigResponse(
    val code: Int,
    val data: RemoteConfigData?,
    val message: String?,
)

data class RemoteConfigData(
    val adType: Int?,
)
