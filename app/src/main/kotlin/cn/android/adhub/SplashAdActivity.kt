package cn.android.adhub

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.data.provider.csj.CsjAdSdkManager
import cn.android.adhub.data.provider.csj.CsjConfig
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo

/**
 * 冷启动开屏 Activity — 使用 AndroidX SplashScreen 作为唯一加载页。
 *
 * 核心思路：
 * 1. 系统 SplashScreen 作为唯一加载页，不再展示 App 自己的 loading panel
 * 2. 在 Activity 内并行拉取远程配置 + 初始化 SDK + 加载开屏广告
 * 3. 广告渲染成功后，让系统 SplashScreen 退场，直接展示广告
 * 4. 超时或失败则跳转主页
 */
class SplashAdActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashAdActivity"
        private const val SPLASH_TIMEOUT_MS = 5000
        private const val FALLBACK_TIMEOUT_MS = 8000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val onCreateTime = System.currentTimeMillis()
    private fun elapsed() = "${System.currentTimeMillis() - onCreateTime}ms"

    private lateinit var adContainer: FrameLayout
    private var finished = false
    private var csjCanJump = false
    private var adReady = false

    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_ad)

        AppContextHolder.init(applicationContext)

        splashScreen.setKeepOnScreenCondition { !adReady }

        adContainer = findViewById(R.id.splash_ad_container)

        Log.d(TAG, "onCreate @${elapsed()}")

        // 兜底超时：8s 后必须进主页
        mainHandler.postDelayed({ onSplashLoadFailed() }, FALLBACK_TIMEOUT_MS)

        // 在 Activity 内启动广告加载流程
        startSplashAdFlow()
    }

    private fun startSplashAdFlow() {
        // 确保隐私同意（首次安装默认同意，与预加载方案保持一致）
        if (!CsjAdSdkManager.isPrivacyAgreed(this)) {
            CsjAdSdkManager.setPrivacyAgreed(this, true)
        }

        // 异步拉取远程配置（供本次或下次使用）
        SplashAdPreloader.fetchRemoteConfigAsync(this)

        // 初始化 SDK 并加载广告
        CsjAdSdkManager.ensureReady(this, object : CsjAdSdkManager.ReadyCallback {
            override fun onReady() {
                Log.d(TAG, "SDK 就绪 @${elapsed()}")
                doLoadSplashAd()
            }

            override fun onFailed() {
                Log.w(TAG, "SDK 初始化失败，进入主页 @${elapsed()}")
                onSplashLoadFailed()
            }
        })
    }

    private fun doLoadSplashAd() {
        if (isFinishing || isDestroyed) {
            onSplashLoadFailed()
            return
        }

        val prefs = getSharedPreferences("ad_remote_config", MODE_PRIVATE)
        val remote = prefs.getString("splash", null)
        val codeId = if (!remote.isNullOrEmpty()) remote else CsjConfig.SPLASH_CODE_ID

        Log.d(TAG, "加载开屏广告: codeId=$codeId @${elapsed()}")

        val dm = resources.displayMetrics
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setExpressViewAcceptedSize(
                dm.widthPixels / dm.density,
                dm.heightPixels / dm.density
            )
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setMediationSplashRequestInfo(csjSplashRequestInfo)
                    .build()
            )
            .build()

        TTAdSdk.getAdManager().createAdNative(this)
            .loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                    Log.i(TAG, "广告加载成功 @${elapsed()}")
                }

                override fun onSplashLoadFail(error: CSJAdError) {
                    Log.e(TAG, "广告加载失败: code=${error.code} msg=${error.msg} @${elapsed()}")
                    onSplashLoadFailed()
                }

                override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                    if (isFinishing || isDestroyed) {
                        onSplashLoadFailed()
                        return
                    }
                    Log.i(TAG, "广告渲染成功 @${elapsed()}")
                    showAd(ad)
                }

                override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                    Log.e(TAG, "广告渲染失败: code=${error.code} msg=${error.msg} @${elapsed()}")
                    onSplashLoadFailed()
                }
            }, SPLASH_TIMEOUT_MS)
    }

    private fun showAd(ad: CSJSplashAd) {
        if (finished || isFinishing) return

        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "展示开屏广告 @${elapsed()}")

        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(ad: CSJSplashAd) {
                Log.i(TAG, "开屏广告已展示 @${elapsed()}")
            }

            override fun onSplashAdClick(ad: CSJSplashAd) {
                Log.d(TAG, "开屏广告被点击")
            }

            override fun onSplashAdClose(ad: CSJSplashAd, closeType: Int) {
                Log.i(TAG, "开屏广告关闭, closeType=$closeType @${elapsed()}")
                mainHandler.post { nextCsj() }
            }
        })

        // 先渲染广告到容器，再让系统 SplashScreen 退场
        ad.showSplashView(adContainer)
        adReady = true
    }

    private fun onSplashLoadFailed() {
        if (finished || isFinishing) return
        Log.w(TAG, "广告加载失败或超时，退出 SplashScreen 并进入主页 @${elapsed()}")
        mainHandler.removeCallbacksAndMessages(null)
        adReady = true
        mainHandler.postDelayed({ goToMain() }, 100)
    }

    private fun nextCsj() {
        if (finished || isFinishing) return
        if (csjCanJump) {
            goToMain()
        } else {
            csjCanJump = true
        }
    }

    private fun goToMain() {
        if (finished || isFinishing) return
        finished = true
        Log.i(TAG, "进入主界面 @${elapsed()}")
        mainHandler.removeCallbacksAndMessages(null)

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("cold_start", true)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (csjCanJump) {
            Log.d(TAG, "onResume 补跳转 CSJ")
            goToMain()
        }
        csjCanJump = true
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause 清除可跳标志 CSJ")
        csjCanJump = false
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
