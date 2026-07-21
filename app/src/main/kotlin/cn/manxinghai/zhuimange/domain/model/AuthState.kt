package cn.manxinghai.zhuimange.domain.model

/**
 * 认证状态。UI 层根据此 sealed class 决定展示登录页 / 主页。
 */
sealed class AuthState {
    /** 未登录（无凭证或已显式登出）。 */
    data object LoggedOut : AuthState()

    /** 已登录，持有有效会话信息。 */
    data class LoggedIn(
        val userId: Int,
        val username: String,
        val accessToken: String,
    ) : AuthState()
}
