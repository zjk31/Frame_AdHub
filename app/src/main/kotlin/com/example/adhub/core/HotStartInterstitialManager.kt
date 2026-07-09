package com.example.adhub.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.adhub.data.AdSdkManager
import com.example.adhub.domain.model.AdPlacement
import com.example.adhub.domain.repository.AdConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 热启动插屏管理器
 *
 * 监听 App 前后台切换，当 App 从后台回到前台时（热启动），
 * 自动展示插屏广告。
 *
 * 防抖策略：
 * - 冷启动后第一次回前台不弹（由开屏广告覆盖）
 * - 两次插屏之间最少间隔 [MIN_INTERVAL_MS]
 * - 后台停留时间需超过 [MIN_BACKGROUND_MS] 才算热启动
 */
class HotStartInterstitialManager(
    private val adSdkManager: AdSdkManager,
    private val adConfigRepo: AdConfigRepository,
    private val pureModeManager: com.example.adhub.core.PureModeManager,
) {

    companion object {
        private const val TAG = "HotStartInterstitial"
        private const val MIN_INTERVAL_MS = 30_000L  // 两次插屏最少间隔 30 秒
        private const val MIN_BACKGROUND_MS = 3_000L // 后台停留超过 3 秒才算热启动
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var lastInterstitialTime = 0L
    private var backgroundedAt = 0L
    private var hasFirstResume = false

    /**
     * 注入的 Activity，用于 showInterstitial 调用。
     * 由 Activity 在 onResume 时设置，onPause 时清除。
     */
    @Volatile
    private var currentActivity: Activity? = null

    fun start() {
        // 监听进程生命周期
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App 进入后台
                backgroundedAt = SystemClock.elapsedRealtime()
                Log.d(TAG, "App entered background at $backgroundedAt")
            }

            override fun onStart(owner: LifecycleOwner) {
                // App 回到前台
                if (!hasFirstResume) {
                    hasFirstResume = true
                    Log.d(TAG, "First resume (cold start), skipping hot-start interstitial")
                    return
                }

                val now = SystemClock.elapsedRealtime()
                val backgroundDuration = now - backgroundedAt

                if (backgroundDuration < MIN_BACKGROUND_MS) {
                    Log.d(TAG, "Background duration too short (${backgroundDuration}ms), skipping")
                    return
                }

                if (now - lastInterstitialTime < MIN_INTERVAL_MS) {
                    Log.d(TAG, "Too soon since last interstitial, skipping")
                    return
                }

                Log.i(TAG, "Hot start detected (background=${backgroundDuration}ms), triggering interstitial")
                showHotStartInterstitial()
            }
        })
    }

    fun bindActivity(activity: Activity) {
        currentActivity = activity
    }

    fun unbindActivity(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    private fun showHotStartInterstitial() {
        val activity = currentActivity ?: run {
            Log.w(TAG, "No activity bound, skipping interstitial")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Activity is finishing/destroyed, skipping")
            return
        }

        // 纯净模式拦截：不展示热启动插屏
        if (pureModeManager.checkActive()) {
            Log.i(TAG, "Pure mode active, skipping hot-start interstitial")
            return
        }

        scope.launch {
            try {
                val codeId = adConfigRepo.getCodeId(AdPlacement.Interstitial)
                if (codeId.isEmpty()) {
                    Log.w(TAG, "Interstitial codeId is empty, skipping")
                    return@launch
                }

                val provider = adSdkManager.currentProvider
                val result = provider.showInterstitial(activity, codeId)

                lastInterstitialTime = SystemClock.elapsedRealtime()
                Log.i(TAG, "Hot-start interstitial result: $result")
            } catch (e: Exception) {
                Log.e(TAG, "Hot-start interstitial failed", e)
            }
        }
    }
}
