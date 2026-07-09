package com.example.adhub.domain.repository

import com.example.adhub.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * 认证 Token 仓储接口，管理 access/refresh token 的持久化与响应式状态。
 *
 * 设计遵循 [AdChannelRepository] 模式：
 * - 同步 val 供 OkHttp Interceptor / Authenticator 在非 suspend 上下文中读取
 * - Flow 供 UI 层响应式观察登录状态
 * - suspend fun 写方法
 */
interface TokenRepository {

    /** 当前缓存的 access token（同步，供 AuthInterceptor 使用）。 */
    val cachedToken: String?

    /** 当前缓存的 refresh token（同步，供 TokenAuthenticator 使用）。 */
    val cachedRefreshToken: String?

    /** 当前登录用户的 ID（同步，供 AdProvider 使用）。 */
    val cachedUserId: Int?

    /** 响应式认证状态流。 */
    val authState: Flow<AuthState>

    /** 当前是否已登录。 */
    val isLoggedIn: Boolean

    /** 登录成功后保存凭证。 */
    suspend fun saveTokens(accessToken: String, refreshToken: String, userId: Int, username: String)

    /** Token 刷新后仅更新 access token。 */
    suspend fun updateAccessToken(accessToken: String)

    /** 登出或刷新失败时清除所有凭证。 */
    suspend fun clearTokens()
}
