package cn.android.adhub.data.remote

import cn.android.adhub.data.remote.dto.ApiResponse
import cn.android.adhub.data.remote.dto.LoginRequest
import cn.android.adhub.data.remote.dto.RefreshRequest
import cn.android.adhub.data.remote.dto.TokenData
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
