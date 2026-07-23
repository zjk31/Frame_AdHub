package cn.manxinghai.zhuimange.data.local

import android.content.Context
import android.content.SharedPreferences
import cn.manxinghai.zhuimange.domain.model.AdChannel

/**
 * 广告 SDK 初始化追踪 — 对齐 flutter_merge AdSdkInitDao / AdSdkInitTables。
 *
 * 记录每次 SDK 初始化的渠道、结果、耗时，用于排查广告填充问题。
 */
object AdSdkInitDao {

    private const val PREFS_NAME = "ad_sdk_init_log"
    private const val KEY_LATEST_CHANNEL = "latest_channel"
    private const val KEY_LATEST_SUCCESS = "latest_success"
    private const val KEY_LATEST_TIME = "latest_time"
    private const val KEY_LATEST_DURATION_MS = "latest_duration_ms"
    private const val KEY_LATEST_ERROR = "latest_error"
    private const val KEY_INIT_COUNT = "init_count"
    private const val KEY_FAIL_COUNT = "fail_count"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 记录一次初始化结果。
     *
     * @param channel  使用的广告渠道
     * @param success  是否初始化成功
     * @param durationMs 初始化耗时（毫秒）
     * @param error    失败时的错误信息
     */
    fun recordInit(
        context: Context,
        channel: AdChannel,
        success: Boolean,
        durationMs: Long,
        error: String? = null
    ) {
        prefs(context).edit().apply {
            putString(KEY_LATEST_CHANNEL, channel.name)
            putBoolean(KEY_LATEST_SUCCESS, success)
            putLong(KEY_LATEST_TIME, System.currentTimeMillis())
            putLong(KEY_LATEST_DURATION_MS, durationMs)
            if (error != null) putString(KEY_LATEST_ERROR, error) else remove(KEY_LATEST_ERROR)
            putInt(KEY_INIT_COUNT, prefs(context).getInt(KEY_INIT_COUNT, 0) + 1)
            if (!success) putInt(KEY_FAIL_COUNT, prefs(context).getInt(KEY_FAIL_COUNT, 0) + 1)
        }.apply()
    }

    /** 最近一次的初始化渠道 */
    fun latestChannel(context: Context): String =
        prefs(context).getString(KEY_LATEST_CHANNEL, "unknown") ?: "unknown"

    /** 最近一次初始化是否成功 */
    fun latestSuccess(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LATEST_SUCCESS, false)

    /** 总初始化次数 */
    fun initCount(context: Context): Int =
        prefs(context).getInt(KEY_INIT_COUNT, 0)

    /** 总失败次数 */
    fun failCount(context: Context): Int =
        prefs(context).getInt(KEY_FAIL_COUNT, 0)

    /** 获取最近一次初始化的摘要信息 */
    fun latestSummary(context: Context): String {
        val p = prefs(context)
        val channel = p.getString(KEY_LATEST_CHANNEL, "unknown") ?: "unknown"
        val success = p.getBoolean(KEY_LATEST_SUCCESS, false)
        val duration = p.getLong(KEY_LATEST_DURATION_MS, 0)
        val error = p.getString(KEY_LATEST_ERROR, null)
        return buildString {
            append("channel=$channel, success=$success, ${duration}ms")
            if (error != null) append(", error=$error")
        }
    }

    /** 清除所有记录（用于重置测试） */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
