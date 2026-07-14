package cn.maga.lingdongmanhua.data.remote

import cn.maga.lingdongmanhua.data.remote.dto.ApiResponse
import cn.maga.lingdongmanhua.data.remote.dto.LoginRequest
import cn.maga.lingdongmanhua.data.remote.dto.RefreshRequest
import cn.maga.lingdongmanhua.data.remote.dto.TokenData
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 认证相关 API 接口。
 *
 * 注意：NetworkClient.BASE_URL 已包含 /api/ 前缀，因此路径只需写 "login" / "refreshToken"。
 */
interface AuthApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenData>

    @POST("refreshToken")
    suspend fun refreshToken(@Body request: RefreshRequest): ApiResponse<TokenData>
}
