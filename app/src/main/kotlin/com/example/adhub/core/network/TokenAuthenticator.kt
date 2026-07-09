package com.example.adhub.core.network

import com.example.adhub.domain.repository.TokenRepository
import com.example.adhub.data.remote.AuthApi
import com.example.adhub.data.remote.dto.RefreshRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator：收到 401 时自动用 refreshToken 刷新并重试原请求。
 *
 * 设计要点：
 * - [runBlocking] 运行在 OkHttp 后台线程，不会阻塞主线程
 * - X-Retry-Count 头防止无限重试循环（最多重试 1 次）
 * - 刷新失败（网络异常 / refreshToken 过期）则清除凭证
 */
class TokenAuthenticator(
    private val tokenRepo: TokenRepository,
    private val authApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 仅处理 401
        if (response.code != 401) return null

        // 防无限重试：检查已重试次数
        val retryCount = response.request.header(HEADER_RETRY_COUNT)?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            runBlocking(Dispatchers.IO) { tokenRepo.clearTokens() }
            return null
        }

        val refreshToken = tokenRepo.cachedRefreshToken
        if (refreshToken.isNullOrBlank()) {
            runBlocking(Dispatchers.IO) { tokenRepo.clearTokens() }
            return null
        }

        return try {
            val refreshResult = runBlocking(Dispatchers.IO) {
                authApi.refreshToken(RefreshRequest(refreshToken))
            }

            if (refreshResult.isSuccess && refreshResult.data != null) {
                val tokenData = refreshResult.data
                // 更新 access token；refreshToken 也同步更新（服务端可能轮换）
                runBlocking(Dispatchers.IO) {
                    tokenRepo.updateAccessToken(tokenData.accessToken)
                    // 如果服务端返回了新的 refreshToken，也一并更新
                    if (tokenData.refreshToken.isNotBlank()) {
                        tokenRepo.saveTokens(
                            accessToken = tokenData.accessToken,
                            refreshToken = tokenData.refreshToken,
                            userId = tokenData.userId,
                            username = "",
                        )
                    }
                }
                // 用新 token 重试原请求
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokenData.accessToken}")
                    .header(HEADER_RETRY_COUNT, "${retryCount + 1}")
                    .build()
            } else {
                runBlocking(Dispatchers.IO) { tokenRepo.clearTokens() }
                null
            }
        } catch (e: Exception) {
            runBlocking(Dispatchers.IO) { tokenRepo.clearTokens() }
            null
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 1
        private const val HEADER_RETRY_COUNT = "X-Retry-Count"
    }
}
