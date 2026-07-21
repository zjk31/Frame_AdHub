package cn.manxinghai.zhuimange

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 穿山甲开屏广告预加载器。
 *
 * 用途：在 [SplashAdActivity] 冷启动流程中提前加载开屏广告，
 * 与 SDK 初始化 / 远程配置拉取并行执行，减少用户等待时间。
 *
 * 使用模式：
 * ```
 * // 1. 尽早启动预加载（可和 SDK init 并行）
 * SplashAdPreloader.preload(context, codeId)
 *
 * // 2. 主流程就绪后尝试消费预加载的广告
 * val preloaded = SplashAdPreloader.consumePendingAd()
 * if (preloaded != null) {
 *     preloaded.showSplashView(container)  // 即时展示
 * } else {
 *     provider.showSplashAd(...)           // 回退正常流程
 * }
 * ```
 *
 * 注意：当前仅支持穿山甲（CSJ）广告通道。
 * 配置拉取和 CSJ SDK 初始化由 [AdConfigRepository] / [CsjAdSdkManager] 负责，本类不重复。
 */
object SplashAdPreloader {
    private const val TAG = "SplashAdPreloader"
    private const val PRELOAD_TIMEOUT_MS = 10_000

    /** GroMore 开屏请求信息（对齐 flutter_merge） */
    private val csjSplashRequestInfo = object : MediationSplashRequestInfo(
        MediationConstant.ADN_PANGLE, "", "", ""
    ) {}

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** 预加载成功的开屏广告（消费后置 null） */
    @Volatile
    var pendingAd: CSJSplashAd? = null
        private set

    /** 预加载是否已完成 */
    @Volatile
    var finished = false
        private set

    /** 预加载是否成功 */
    @Volatile
    var success = false
        private set

    /**
     * 启动预加载。
     *
     * 在 [CsjAdSdkManager] 已初始化且 [codeId] 已知后调用。
     * 内部在 IO 线程执行广告加载 + 渲染，不阻塞调用线程。
     */
    fun preload(context: Context, codeId: String) {
        if (codeId.isEmpty()) {
            Log.w(TAG, "codeId 为空，跳过预加载")
            finished = true
            success = false
            return
        }
        pendingAd = null
        finished = false
        success = false

        Log.i(TAG, "预加载启动: codeId=$codeId")
        ioExecutor.execute {
            doPreload(context.applicationContext, codeId)
        }
    }

    private fun doPreload(context: Context, codeId: String) {
        try {
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
                        .setExtraObject("show_adn_load_error_detail", true)
                        .build()
                )
                .build()

            val adNative = TTAdSdk.getAdManager().createAdNative(context)
            val latch = java.util.concurrent.CountDownLatch(1)

            adNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                    Log.d(TAG, "预加载: 广告加载成功")
                }

                override fun onSplashLoadFail(error: CSJAdError) {
                    Log.e(TAG, "预加载: 广告加载失败 code=${error.code} msg=${error.msg}")
                    finished = true
                    success = false
                    latch.countDown()
                }

                override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                    Log.i(TAG, "预加载: 广告渲染成功")
                    pendingAd = ad
                    finished = true
                    success = true
                    latch.countDown()
                }

                override fun onSplashRenderFail(ad: CSJSplashAd, error: CSJAdError) {
                    Log.e(TAG, "预加载: 广告渲染失败 code=${error.code} msg=${error.msg}")
                    finished = true
                    success = false
                    latch.countDown()
                }
            }, PRELOAD_TIMEOUT_MS)

            // 等待预加载完成（最长 PRELOAD_TIMEOUT_MS）
            latch.await()
        } catch (e: Exception) {
            Log.e(TAG, "预加载异常: ${e.message}", e)
            finished = true
            success = false
        }
    }

    /**
     * 消费预加载的广告（取走即置 null，不可重复使用）。
     *
     * @return 预加载成功的 [CSJSplashAd]，或 null（未就绪 / 已消费 / 失败）
     */
    fun consumePendingAd(): CSJSplashAd? {
        val ad = pendingAd
        pendingAd = null
        if (ad != null) {
            Log.i(TAG, "预加载广告已消费")
        }
        return ad
    }

    /**
     * 重置预加载器状态。
     */
    fun reset() {
        pendingAd = null
        finished = false
        success = false
        Log.d(TAG, "预加载器已重置")
    }
}
