package cn.maga.lingdongmanhua.data.api

import cn.maga.lingdongmanhua.data.dto.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 百度 OCPC 回调 API
 * 用于广告转化追踪
 */
interface BaiduOcpcApi {

    @GET("api/ocpc/baidu/callback")
    suspend fun baiduOcpcCallback(
        @Query("clickId") clickId: String,
        @Query("actionType") actionType: String,  // e.g. "activate", "register", "pay"
        @Query("value") value: String? = null
    ): ApiResponse<Boolean>
}
