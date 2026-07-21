package cn.manxinghai.zhuimange.core.auth

/**
 * 通用用户模型 — 与业务无关的最小用户表示。
 *
 * 子 App 可扩展此模型或在 ViewModel 层映射为 UI 专用模型。
 *
 * @param id 用户 ID（0 或负数 = 未登录/无效）
 * @param username 用户名（游客以 "guest_" 开头）
 * @param nickname 昵称（可选）
 * @param token 当前 access token
 * @param extra 子 App 自定义扩展字段
 */
data class AppUser(
    val id: Long,
    val username: String,
    val nickname: String = "",
    val token: String = "",
    val extra: Map<String, String> = emptyMap(),
) {
    /** 是否为游客（username 以 guest_ 开头） */
    val isGuest: Boolean get() = username.startsWith("guest_")

    /** 是否真正登录（非游客 + id > 0） */
    val isRealLogin: Boolean get() = id > 0 && !isGuest
}
