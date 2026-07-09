package com.example.adhub.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 纯净模式管理器
 *
 * 用户通过观看「纯净激励视频」获得免广告权益。
 * 在纯净模式激活期间，所有广告位不展示广告。
 *
 * 权益以时间窗口形式管理：
 * - 每次观看纯净激励视频后，获得 [REWARD_DURATION_HOURS] 小时的免广告权益
 * - 权益到期后自动恢复广告展示
 */
class PureModeManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "pure_mode_prefs"
        private const val KEY_EXPIRE_TIMESTAMP = "expire_timestamp"
        private const val REWARD_DURATION_HOURS = 2L  // 每次纯净激励获得 2 小时免广告
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPureModeActive = MutableStateFlow(checkActive())
    val isPureModeActive: StateFlow<Boolean> = _isPureModeActive.asStateFlow()

    /**
     * 检查纯净模式是否激活（权益未过期）。
     */
    fun checkActive(): Boolean {
        val expire = prefs.getLong(KEY_EXPIRE_TIMESTAMP, 0L)
        return System.currentTimeMillis() < expire
    }

    /**
     * 获取剩余免广告时间（毫秒），0 表示已过期。
     */
    fun getRemainingTimeMs(): Long {
        val expire = prefs.getLong(KEY_EXPIRE_TIMESTAMP, 0L)
        val remaining = expire - System.currentTimeMillis()
        return remaining.coerceAtLeast(0L)
    }

    /**
     * 用户完成纯净激励视频观看，授予免广告权益。
     * 若当前已有权益，则叠加延长。
     */
    fun grantReward() {
        val currentExpire = prefs.getLong(KEY_EXPIRE_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        val base = maxOf(currentExpire, now)  // 叠加或重新开始
        val newExpire = base + REWARD_DURATION_HOURS * 60 * 60 * 1000L

        prefs.edit().putLong(KEY_EXPIRE_TIMESTAMP, newExpire).apply()
        _isPureModeActive.value = true
    }

    /**
     * 手动撤销纯净模式（用于测试或用户主动关闭）。
     */
    fun revoke() {
        prefs.edit().putLong(KEY_EXPIRE_TIMESTAMP, 0L).apply()
        _isPureModeActive.value = false
    }

    /**
     * 刷新状态（可在 App 回前台时调用，确保过期后状态更新）。
     */
    fun refresh() {
        _isPureModeActive.value = checkActive()
    }
}
