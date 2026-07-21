package cn.manxinghai.zhuimange.data.local

import android.content.Context
import android.content.SharedPreferences
import cn.manxinghai.zhuimange.domain.model.AuthState
import cn.manxinghai.zhuimange.domain.repository.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [TokenRepository] 的 SharedPreferences 实现。
 *
 * 遵循 [AdChannelStore] 模式：专用 prefs 文件 + MutableStateFlow 响应式状态 + companion key 常量。
 */
class TokenStore(context: Context) : TokenRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(loadFromPrefs())
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    // ── 同步读取（供 OkHttp Interceptor / Authenticator 使用） ──

    override val cachedToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)

    override val cachedRefreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)

    override val cachedUserId: Int?
        get() = prefs.getInt(KEY_USER_ID, -1).takeIf { it >= 0 }

    override val isLoggedIn: Boolean
        get() = _authState.value is AuthState.LoggedIn

    // ── 写操作 ──

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: Int,
        username: String,
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
        _authState.value = AuthState.LoggedIn(userId, username, accessToken)
    }

    override suspend fun updateAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
        val current = _authState.value
        if (current is AuthState.LoggedIn) {
            _authState.value = current.copy(accessToken = accessToken)
        }
    }

    override suspend fun clearTokens() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.LoggedOut
    }

    // ── 内部方法 ──

    private fun loadFromPrefs(): AuthState {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return AuthState.LoggedOut
        val userId = prefs.getInt(KEY_USER_ID, -1)
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        return if (userId >= 0) {
            AuthState.LoggedIn(userId, username, token)
        } else {
            AuthState.LoggedOut
        }
    }

    companion object {
        private const val PREFS_NAME = "auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }
}
