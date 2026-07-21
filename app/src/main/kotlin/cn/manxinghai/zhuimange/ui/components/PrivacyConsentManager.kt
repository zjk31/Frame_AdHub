package cn.manxinghai.zhuimange.ui.components

import android.content.Context

/**
 * 隐私同意状态管理器。
 *
 * 子 App 在 [SplashAdActivity] 中调用 [showIfNeeded] 即可完成首次合规流程。
 * CSJ 等聚合 SDK 强制要求隐私同意后初始化——在同意前不得调用任何 SDK API。
 */
object PrivacyConsentManager {
    private const val PREFS_NAME = "privacy_consent"
    private const val KEY_AGREED = "agreed"

    /** 用户是否已同意隐私政策。 */
    fun isAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AGREED, false)

    /** 记录用户同意。 */
    fun setAgreed(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AGREED, true).apply()
    }

    /** 撤销同意（用于测试/设置页面）。 */
    fun revoke(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AGREED, false).apply()
    }
}
