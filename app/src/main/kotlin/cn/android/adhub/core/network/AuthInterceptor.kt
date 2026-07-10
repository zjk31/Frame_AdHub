package cn.android.adhub.core.network

import cn.android.adhub.domain.repository.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor：在每个请求中注入 Authorization header。
 *
 * 对登录和刷新端点跳过注入（这些端点不需要 token，发送过期 token 可能导致意外行为）。
 */
class AuthInterceptor(
    private val tokenRepo: TokenRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 登录和刷新端点无需携带 token
        val path = originalRequest.url.encodedPath
        if (path.endsWith("/login") || path.endsWith("/refreshToken")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenRepo.cachedToken
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
