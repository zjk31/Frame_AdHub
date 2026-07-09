package com.example.adhub.data.remote.dto

/**
 * 登录请求体。
 */
data class LoginRequest(
    val username: String,
    val password: String,
)

/**
 * Token 刷新请求体。
 */
data class RefreshRequest(
    val refreshToken: String,
)

/**
 * 认证 Token 响应数据（位于 ApiResponse.data 内）。
 */
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val userId: Int,
)
