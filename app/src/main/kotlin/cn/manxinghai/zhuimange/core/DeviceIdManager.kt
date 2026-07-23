package cn.manxinghai.zhuimange.core

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.gzuliyujiang.oaid.IGetter
import java.util.UUID

/**
 * 设备标识管理器
 * 获取优先级：OAID > AndroidID > 持久化 UUID
 */
object DeviceIdManager {

    private const val TAG = "DeviceIdManager"
    private const val PREFS_NAME = "device_id_prefs"
    private const val KEY_UUID = "persistent_uuid"
    private const val KEY_OAID = "cached_oaid"

    @Volatile
    private var oaid: String? = null

    @Volatile
    private var initialized = false

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            oaid = prefs.getString(KEY_OAID, null)
            initialized = true
            Log.i(TAG, "DeviceIdManager initialized, cached oaid=${if (oaid.isNullOrBlank()) "empty" else "***"}")

            // 异步获取 OAID（Android_CN_OAID）
            try {
                DeviceID.getOAID(appContext, object : IGetter {
                    override fun onOAIDGetComplete(result: String) {
                        setOaid(result)
                    }
                    override fun onOAIDGetError(error: Exception) {
                        Log.w(TAG, "OAID fetch failed: ${error.message}")
                        setOaid(null)
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "OAID init failed: ${e.message}")
            }
        }
    }

    private fun setOaid(value: String?) {
        val clean = value?.takeIf { it.isNotBlank() && !it.contains("00000000") && it.length > 3 }
        oaid = clean
        if (clean != null) {
            prefs.edit().putString(KEY_OAID, clean).apply()
            Log.i(TAG, "OAID obtained and cached")
        } else {
            Log.i(TAG, "OAID unavailable, fallback to AndroidID/UUID, raw=${value?.take(8)}...")
        }
    }

    /**
     * 获取设备唯一标识（不需要 Context）
     * 1. 优先 OAID
     * 2. 其次 AndroidID
     * 3. 最后用持久化 UUID
     */
    fun getDeviceId(): String {
        if (!initialized) return UUID.randomUUID().toString()

        // 1. OAID
        oaid?.let { if (it.isNotBlank()) return it }

        // 2. AndroidID
        val androidId = try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { null }

        if (!androidId.isNullOrBlank() &&
            androidId != "9774d56d682e549c" &&
            !androidId.matches(Regex("^0+$"))
        ) {
            return androidId
        }

        // 3. 持久化 UUID
        return getOrCreateUuid()
    }

    private fun getOrCreateUuid(): String {
        var uuid = prefs.getString(KEY_UUID, null)
        if (uuid.isNullOrBlank()) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_UUID, uuid).apply()
            Log.i(TAG, "Generated new persistent UUID")
        }
        return uuid
    }

    fun getAndroidId(): String {
        if (!initialized) return ""
        return try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { "" }
    }
}
