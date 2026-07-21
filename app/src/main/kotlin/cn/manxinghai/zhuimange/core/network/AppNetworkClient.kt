package cn.manxinghai.zhuimange.core.network

import android.os.Build
import cn.manxinghai.zhuimange.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 通用 Retrofit 客户端工厂。
 *
 * 与业务无关——只提供 OkHttp / Retrofit 构建能力。
 * 子 App 通过 [createApi] 一行创建 API 代理，
 * 也可用 [buildClient] 自建 OkHttp 实例（如 SSE 长连接）。
 *
 * # 使用示例
 * ```kotlin
 * val api = AppNetworkClient.createApi<MyApi>(
 *     baseUrl = BuildConfig.APP_BASE_URL,
 *     extraInterceptors = listOf(myAuthInterceptor),
 * )
 * ```
 */
object AppNetworkClient {

    /** 默认连接超时（秒） */
    var connectTimeoutSec: Long = 15
    /** 默认读取超时（秒，SSE 等长连接场景应设为 0） */
    var readTimeoutSec: Long = 20

    // ── User-Agent ──

    /** User-Agent 模板，首次访问时懒加载（含设备信息，不走主线程）。 */
    val userAgent: String by lazy {
        "manxinghai/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Locale.getDefault().toLanguageTag()})"
    }

    // ── 核心构建方法 ──

    /**
     * 构建 OkHttpClient。
     *
     * 内置：连接/读取超时 + 日志拦截器(debug: BODY, release: NONE)。
     * [extraInterceptors] 按子 App 需要注入（如 Token Header）。
     * [readTimeoutSec] 设为 0 表示无限等待（SSE 场景）。
     */
    fun buildClient(
        vararg extraInterceptors: Interceptor,
        readTimeoutSec: Long = this.readTimeoutSec,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .apply { extraInterceptors.forEach { addInterceptor(it) } }
            .build()
    }

    /**
     * 构建 Retrofit 实例。
     *
     * [baseUrl] 必须以 "/" 结尾。
     */
    fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonFactory.create()))
            .build()
    }

    /**
     * 快捷方法：构建 OkHttpClient + Retrofit → create(service)。
     *
     * 适用于大多数场景，一行代码拿到 API 代理。
     */
    inline fun <reified T> createApi(
        baseUrl: String,
        vararg extraInterceptors: Interceptor,
        readTimeoutSec: Long = this.readTimeoutSec,
    ): T {
        val client = buildClient(*extraInterceptors, readTimeoutSec = readTimeoutSec)
        val retrofit = buildRetrofit(baseUrl, client)
        return retrofit.create(T::class.java)
    }

    // ── 实例缓存（单 baseUrl） ──

    @Volatile
    private var cachedRetrofit: Retrofit? = null
    private var cachedBaseUrl: String = ""

    /**
     * 获取/创建缓存的 Retrofit（同 baseUrl 复用连接池）。
     */
    fun getOrCreateCached(baseUrl: String, vararg extraInterceptors: Interceptor): Retrofit {
        val current = cachedRetrofit
        if (current != null && cachedBaseUrl == baseUrl) return current
        return synchronized(this) {
            if (cachedRetrofit != null && cachedBaseUrl == baseUrl) {
                cachedRetrofit!!
            } else {
                val client = buildClient(*extraInterceptors)
                buildRetrofit(baseUrl, client).also {
                    cachedRetrofit = it
                    cachedBaseUrl = baseUrl
                }
            }
        }
    }
}
