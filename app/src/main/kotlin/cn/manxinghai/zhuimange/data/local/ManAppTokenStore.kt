package cn.manxinghai.zhuimange.data.local

import android.content.Context
import android.content.SharedPreferences
import cn.manxinghai.zhuimange.data.repository.TokenStore
import cn.manxinghai.zhuimange.domain.model.User
import com.google.gson.Gson

/**
 * ManApp Token & User 缓存
 * 使用 SharedPreferences 存储 token 和用户信息
 */
class ManAppTokenStore(context: Context) : TokenStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    override fun getCachedUserInfo(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try { gson.fromJson(json, User::class.java) } catch (_: Exception) { null }
    }

    override fun saveCachedUserInfo(user: User) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    override fun getCachedUserId(): Long? = getCachedUserInfo()?.id

    companion object {
        private const val PREFS_NAME = "manapp_token_prefs"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USER = "cached_user"
    }
}
