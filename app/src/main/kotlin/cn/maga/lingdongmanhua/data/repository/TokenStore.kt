package cn.maga.lingdongmanhua.data.repository

import cn.maga.lingdongmanhua.domain.model.User

/**
 * 漫画 App Token 存储接口（独立于广告框架的 TokenStore）
 */
interface TokenStore {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
    fun getCachedUserInfo(): User?
    fun saveCachedUserInfo(user: User)
    fun getCachedUserId(): Long?
}
