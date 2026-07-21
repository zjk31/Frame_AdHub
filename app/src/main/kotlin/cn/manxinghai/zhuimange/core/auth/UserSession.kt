package cn.manxinghai.zhuimange.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户会话管理器 — 通用骨架，无业务依赖。
 *
 * 用法：
 * ```
 * val session = UserSession(MyTokenStore(context), MyUserCache(context))
 * session.saveUser(appUser)    // 登录后调用
 * session.currentUser          // StateFlow<AppUser?>
 * session.isLoggedIn           // 同步查询
 * session.logout()             // 退出登录
 * ```
 *
 * 子 App 需要提供两个简单接口的实现：
 * - [TokenStorage]：token 持久化（SharedPrefs / DataStore / …）
 * - [UserCache]：用户信息缓存（可选，传 [NoopUserCache] 不缓存）
 */
class UserSession(
    private val tokenStorage: TokenStorage,
    private val userCache: UserCache = NoopUserCache,
) {
    private val _currentUser = MutableStateFlow<AppUser?>(userCache.load())
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean get() = _currentUser.value != null && _currentUser.value!!.id > 0

    val token: String? get() = tokenStorage.get()

    fun isGuest(): Boolean = _currentUser.value?.isGuest ?: true

    fun isRealLogin(): Boolean = _currentUser.value?.isRealLogin ?: false

    /** 登录成功后保存用户状态 */
    fun saveUser(user: AppUser) {
        tokenStorage.save(user.token)
        if (user.id > 0) userCache.save(user)
        _currentUser.value = user
    }

    /** 退出登录 */
    fun logout() {
        tokenStorage.clear()
        userCache.clear()
        _currentUser.value = null
    }
}

// ═══════════════════════════════════════════════════════
//  子 App 需实现的可插拔接口
// ═══════════════════════════════════════════════════════

/** Token 持久化接口 */
interface TokenStorage {
    fun get(): String?
    fun save(token: String)
    fun clear()
}

/** 用户信息缓存接口 */
interface UserCache {
    fun load(): AppUser?
    fun save(user: AppUser)
    fun clear()
}

/** 不缓存用户信息（仅内存） */
object NoopUserCache : UserCache {
    override fun load(): AppUser? = null
    override fun save(user: AppUser) {}
    override fun clear() {}
}
