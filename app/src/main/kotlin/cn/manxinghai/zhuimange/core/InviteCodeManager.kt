package cn.manxinghai.zhuimange.core

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import java.util.UUID

/**
 * 邀请码管理器 — 对齐 Flutter InviteCodeHelper + NativeInviteCodeBridge
 *
 * 优先级：
 * 1. 服务端返回的 inviteCode（[set] 写入，登录后覆盖）
 * 2. 友盟 UMID（设备级，卸装不变）
 * 3. MD5(time) 兜底
 */
object InviteCodeManager {
    private const val TAG = "InviteCodeManager"
    private const val PREFS_NAME = "invite_code_prefs"
    private const val KEY_INVITE_CODE = "invite_code"

    @Volatile
    private var cachedCode: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_INVITE_CODE, null)
        if (code.isNullOrBlank()) {
            code = generateInviteCode(context)
            if (!code.isNullOrBlank()) {
                prefs.edit().putString(KEY_INVITE_CODE, code).apply()
            }
        }
        cachedCode = code
    }

    /** 生成 inviteCode：优先 UMID，不行则 UUID */
    private fun generateInviteCode(context: Context): String {
        // 1. 尝试友盟 UMID
        try {
            val umid = com.umeng.commonsdk.UMConfigure.getUMIDString(context)
            if (!umid.isNullOrBlank() && umid != "NULL") {
                Log.i(TAG, "使用友盟 UMID: $umid")
                return umid
            }
        } catch (_: Throwable) {
            Log.w(TAG, "UMConfigure.getUMIDString 失败")
        }
        // 2. 兜底：MD5(custom_id_ + 时间戳)
        val raw = "custom_id_" + System.currentTimeMillis()
        val md5 = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        Log.i(TAG, "UMID 为空，使用 MD5: $md5")
        return md5
    }

    /** 获取当前 inviteCode */
    fun get(): String {
        cachedCode?.let { return it }
        return UUID.randomUUID().toString().replace("-", "")
    }

    /** UM SDK 就绪后重试获取 UMID */
    fun tryRefreshFromUmeng(context: Context) {
        try {
            val umid = com.umeng.commonsdk.UMConfigure.getUMIDString(context)
            if (!umid.isNullOrBlank() && umid != "NULL" && umid != cachedCode) {
                Log.i(TAG, "UM SDK 就绪，更新为 UMID: $umid")
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_INVITE_CODE, umid).apply()
                cachedCode = umid
            }
        } catch (_: Throwable) {}
    }

    /** 用服务端 inviteCode 覆盖本地值（登录/注册后调用） */
    fun set(code: String, context: Context) {
        if (code.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_INVITE_CODE, code).apply()
        cachedCode = code
    }

    /** 退出登录时强制重新生成 inviteCode */
    fun forceRegenerate(context: Context) {
        val raw = "custom_id_" + System.currentTimeMillis()
        val md5 = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        Log.i(TAG, "退出登录，重新生成 inviteCode: $md5")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_INVITE_CODE, md5).apply()
        cachedCode = md5
    }
}
