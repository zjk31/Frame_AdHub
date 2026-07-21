package cn.manxinghai.zhuimange.data.provider.csj

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdSdk

/**
 * 穿山甲 SDK 管理器 — 静态单例，对齐 flutter_merge 的 CsjAdSdkManager.java。
 *
 * 核心流程：
 * 1. initIfNeeded → TTAdSdk.init（同步，仅一次）
 * 2. ensureReady → TTAdSdk.start（异步回调）+ waitForReady 轮询
 * 3. 隐私同意状态通过 SharedPreferences 持久化
 *
 * GroMore 聚合模式：useMediation(true) + setMediationAdSlot。
 */
object CsjAdSdkManager {
    private const val TAG = "CsjAdSdkManager"
    private const val PREFS_NAME = "ad_privacy"
    private const val KEY_PRIVACY_AGREED = "csj_privacy_agreed"

    @Volatile private var initialized = false
    @Volatile private var starting = false
    @Volatile private var started = false

    private val mainHandler = Handler(Looper.getMainLooper())

    fun isPrivacyAgreed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PRIVACY_AGREED, false)
    }

    fun setPrivacyAgreed(context: Context, agreed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRIVACY_AGREED, agreed).apply()
    }

    @Synchronized
    fun initIfNeeded(context: Context) {
        if (initialized || !isPrivacyAgreed(context)) return
        val config = CsjConfig.buildAdConfig(context)
        val result = TTAdSdk.init(context.applicationContext, config)
        Log.i(TAG, "TTAdSdk.init result=$result appId=${CsjConfig.APP_ID} isSdkReady=${TTAdSdk.isSdkReady()}")
        initialized = true
    }

    interface ReadyCallback {
        fun onReady()
        fun onFailed()
    }

    fun ensureReady(context: Context, callback: ReadyCallback) {
        if (!isPrivacyAgreed(context)) {
            Log.e(TAG, "用户未同意隐私政策，无法初始化穿山甲 SDK")
            callback.onFailed()
            return
        }
        initIfNeeded(context)
        synchronized(this) {
            if (started && TTAdSdk.isSdkReady()) {
                Log.i(TAG, "穿山甲 SDK 已初始化完成")
                callback.onReady()
                return
            }
            if (starting) {
                Log.i(TAG, "穿山甲 SDK 正在初始化中")
                waitForReady(callback, 0)
                return
            }
            starting = true
        }
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                Log.i(TAG, "穿山甲 SDK 初始化成功")
                synchronized(this@CsjAdSdkManager) {
                    started = true
                    starting = false
                }
                waitForReady(callback, 0)
            }
            override fun fail(code: Int, msg: String) {
                Log.e(TAG, "穿山甲 SDK 初始化失败: $code, $msg")
                synchronized(this@CsjAdSdkManager) {
                    started = false
                    starting = false
                }
                callback.onFailed()
            }
        })
    }

    /** 轮询 isSdkReady（最多 20 次 × 120ms = 2.4s）。 */
    private fun waitForReady(callback: ReadyCallback, tries: Int) {
        if (TTAdSdk.isSdkReady()) {
            synchronized(this) { started = true; starting = false }
            Log.i(TAG, "SDK ready at try=$tries")
            callback.onReady()
            return
        }
        if (tries >= 20) {
            synchronized(this) { starting = false }
            Log.e(TAG, "SDK ready TIMEOUT after $tries tries")
            callback.onFailed()
            return
        }
        mainHandler.postDelayed({ waitForReady(callback, tries + 1) }, 120)
    }

    fun revokePrivacyConsent(context: Context) {
        setPrivacyAgreed(context, false)
        synchronized(this) {
            initialized = false
            starting = false
            started = false
        }
    }

    fun isReady(): Boolean = try { TTAdSdk.isSdkReady() } catch (_: Exception) { false }
}
