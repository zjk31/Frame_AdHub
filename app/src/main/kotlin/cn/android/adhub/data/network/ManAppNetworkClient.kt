package cn.android.adhub.data.network

import cn.android.adhub.BuildConfig
import cn.android.adhub.core.AppContextHolder
import cn.android.adhub.data.repository.TokenStore
import cn.android.adhub.domain.model.User
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ManApp 独立网络客户端
 * Base URL: http://192.168.2.184:8123
 * 独立于广告框架的 AuthInterceptor，避免循环依赖
 */
object ManAppNetworkClient {

    private const val BASE_URL = "http://192.168.2.184:8123"
    private const val CONNECT_TIMEOUT = 5L
    private const val READ_TIMEOUT = 10L

    /** 获取 Base URL（供 SSE 等模块使用） */
    fun getBaseUrl(): String = BASE_URL

    @Volatile
    private var _instance: Retrofit? = null

    fun getInstance(tokenStore: TokenStore): Retrofit {
        return _instance ?: synchronized(this) {
            _instance ?: buildRetrofit(tokenStore).also { _instance = it }
        }
    }

    private fun buildRetrofit(tokenStore: TokenStore): Retrofit {
        val client = buildOkHttpClient(tokenStore)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonFactory.create()))
            .build()
    }

    private fun buildOkHttpClient(tokenStore: TokenStore): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(ManAppHeaderInterceptor(tokenStore))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
        return builder.build()
    }

    /**
     * 请求头拦截器
     * - version: 应用版本号
     * - appName: 包名
     * - access-token: 登录后携带（登录接口除外）
     * - inviteCode: 设备标识
     */
    private class ManAppHeaderInterceptor(
        private val tokenStore: TokenStore
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val path = original.url.encodedPath

            // 登录/注册接口不携带 token
            val needsToken = path != "/api/user/login" && path != "/api/user/registerDirect"

            val builder = original.newBuilder()
                .header("version", BuildConfig.VERSION_NAME)
                .header("appName", BuildConfig.APPLICATION_ID)
                .header("inviteCode", getDeviceId())

            if (needsToken) {
                val token = runBlocking { getTokenSync() }
                if (!token.isNullOrBlank()) {
                    builder.header("access-token", token)
                }
            }

            return chain.proceed(builder.build())
        }

        private fun getTokenSync(): String? {
            return try {
                tokenStore.getToken()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun getDeviceId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                AppContextHolder.context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        } catch (_: Exception) {
            "unknown"
        }
    }
}

/**
 * Gson 工厂（统一 Gson 配置）
 */
object GsonFactory {
    @Volatile
    private var _gson: Gson? = null

    fun create(): Gson {
        return _gson ?: synchronized(this) {
            _gson ?: com.google.gson.GsonBuilder()
                .setLenient()
                .create()
                .also { _gson = it }
        }
    }
}
