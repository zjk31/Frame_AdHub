package cn.android.adhub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import cn.android.adhub.core.PureModeManager
import cn.android.adhub.data.AdSdkManager
import cn.android.adhub.domain.model.AdPlacement
import cn.android.adhub.domain.repository.AdConfigRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject

class SplashAdActivity : ComponentActivity() {

    private val adSdkManager: AdSdkManager by inject()
    private val adConfigRepo: AdConfigRepository by inject()
    private val pureModeManager: PureModeManager by inject()

    private var adContainer: View? = null
    private var loadingPanel: View? = null
    private var finished = false

    companion object {
        private const val TAG = "SplashAdActivity"
        private const val FALLBACK_TIMEOUT_MS = 8000L
    }

    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "协程异常: ${throwable.message}", throwable)
        goToHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate 开始")
        setContentView(R.layout.activity_splash_ad)

        adContainer = findViewById(R.id.splash_ad_container)
        loadingPanel = findViewById(R.id.splash_loading_panel)
        adContainer?.visibility = View.GONE
        loadingPanel?.visibility = View.VISIBLE

        // 超时降级
        (lifecycleScope + ceh).launch {
            Log.d(TAG, "超时协程已启动, timeout=${FALLBACK_TIMEOUT_MS}ms")
            delay(FALLBACK_TIMEOUT_MS)
            if (!finished) {
                Log.w(TAG, "开屏超时，降级进入主页")
                goToHome()
            }
        }

        // 冷启动流程
        (lifecycleScope + ceh).launch {
            Log.d(TAG, "冷启动协程已启动")
            try {
                startColdFlow()
            } catch (e: Exception) {
                Log.e(TAG, "startColdFlow 异常: ${e.message}", e)
                goToHome()
            }
        }
    }

    private suspend fun startColdFlow() {
        Log.d(TAG, "startColdFlow 开始, 检查纯净模式...")

        if (pureModeManager.checkActive()) {
            Log.i(TAG, "纯净模式已激活，跳过开屏广告")
            goToHome()
            return
        }
        Log.d(TAG, "非纯净模式，继续")

        // 确保 SDK 就绪（AdHubApp 已异步初始化，此处仅等待）
        Log.d(TAG, "等待 SDK 就绪...")
        try {
            val provider = adSdkManager.currentProvider
            // 轮询等待，最多 5 秒
            repeat(50) {
                if (provider.isReady()) return@repeat
                kotlinx.coroutines.delay(100)
            }
            if (!provider.isReady()) {
                Log.w(TAG, "SDK 仍未就绪，继续尝试展示广告")
            }
        } catch (e: Exception) {
            Log.e(TAG, "等待 SDK 就绪异常: ${e.message}", e)
            goToHome()
            return
        }
        Log.d(TAG, "SDK 就绪")

        // 获取代码位
        val codeId = adConfigRepo.getCodeId(AdPlacement.Splash)
        Log.d(TAG, "开屏代码位: $codeId")
        if (codeId.isEmpty()) {
            Log.w(TAG, "开屏代码位为空，跳过")
            goToHome()
            return
        }

        val provider = adSdkManager.currentProvider
        Log.d(TAG, "当前通道: ${provider.javaClass.simpleName}")

        // 隐藏加载面板
        runOnUiThread {
            loadingPanel?.visibility = View.GONE
            adContainer?.let { ct ->
                ct.visibility = View.VISIBLE
                ct.setBackgroundColor(0xFFF5F5F5.toInt())
            }
        }

        try {
            Log.i(TAG, "开始展示开屏广告...")
            val shown = provider.showSplashAd(this, codeId)
            Log.i(TAG, if (shown) "开屏广告已关闭" else "开屏广告未展示（无填充或渲染失败）")
        } catch (e: Exception) {
            Log.e(TAG, "开屏广告异常: ${e.message}", e)
        }

        goToHome()
    }

    private fun goToHome() {
        if (finished || isFinishing || isDestroyed) return
        finished = true
        Log.i(TAG, "进入主页")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        adContainer = null
        loadingPanel = null
    }
}
