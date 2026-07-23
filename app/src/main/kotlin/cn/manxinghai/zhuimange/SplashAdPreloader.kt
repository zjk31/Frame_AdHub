package cn.manxinghai.zhuimange

import android.content.Context
import android.util.Log
import cn.manxinghai.zhuimange.data.provider.csj.CsjAdSdkManager
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationSplashRequestInfo

/**
 * 穿山甲开屏广告预加载器。
 *
 * 子 App 在 Application.onCreate 中调用 [start] 提前加载开屏广告，
 * SplashAdActivity 通过 [consumePendingAd] 消费预加载的广告。
 */
object SplashAdPreloader {
    private const val TAG = "SplashAdPreloader"
    private const val SPLASH_TIMEOUT_MS = 5000

    /** 预加载的开屏广告（渲染成功，待展示） */
    @Volatile
    var pendingCsjSplash: CSJSplashAd? = null
        private set

    /** 预加载是否已完成 */
    @Volatile
    var preloadFinished = false
        private set

    /** 预加载是否成功 */
    @Volatile
    var preloadSuccess = false
        private set

    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    /**
     * 启动预加载。
     *
     * @param codeId 开屏代码位（子 App 自行决定来源：远程配置/硬编码）
     */
    fun start(context: Context, codeId: String) {
        Log.i(TAG, "预加载启动")
        pendingCsjSplash = null
        preloadFinished = false
        preloadSuccess = false

        if (!CsjAdSdkManager.isPrivacyAgreed(context)) {
            CsjAdSdkManager.setPrivacyAgreed(context, true)
        }

        CsjAdSdkManager.ensureReady(context, object : CsjAdSdkManager.ReadyCallback {
            override fun onReady() {
                Log.i(TAG, "SDK 就绪，开始预加载开屏广告: codeId=$codeId")
                preloadSplashAd(context, codeId)
            }

            override fun onFailed() {
                Log.w(TAG, "SDK 初始化失败，预加载终止")
                preloadFinished = true
                preloadSuccess = false
            }
        })
    }

    private fun preloadSplashAd(context: Context, codeId: String) {
        val dm = context.resources.displayMetrics
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

        val adNative = TTAdSdk.getAdManager().createAdNative(context)

        adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                Log.i(TAG, "预加载: 广告加载成功")
            }

            override fun onSplashLoadFail(error: CSJAdError) {
                Log.e(TAG, "预加载: 广告加载失败 code=${error.code} msg=${error.msg}")
                preloadFinished = true
                preloadSuccess = false
            }

            override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                Log.i(TAG, "预加载: 广告渲染成功")
                pendingCsjSplash = ad
                preloadFinished = true
                preloadSuccess = true
            }

            override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                Log.e(TAG, "预加载: 广告渲染失败 code=${error.code} msg=${error.msg}")
                preloadFinished = true
                preloadSuccess = false
            }
        }, SPLASH_TIMEOUT_MS)
    }

    /**
     * 消费预加载的广告（取走后不可再用）。
     * @return 预加载的 CSJSplashAd，或 null（未就绪/已消费/失败）
     */
    fun consumePendingAd(): CSJSplashAd? {
        val ad = pendingCsjSplash
        pendingCsjSplash = null
        return ad
    }
}
