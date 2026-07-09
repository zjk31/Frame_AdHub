package com.example.adhub.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 全局网络客户端工厂，由 Koin DI 管理为单例。
 *
 * 拦截器链顺序：AuthInterceptor → HttpLoggingInterceptor → 网络
 * Authenticator 处理 401 响应并自动刷新 token。
 */
class NetworkClient(
    private val authInterceptor: AuthInterceptor,
    private val tokenAuthenticator: TokenAuthenticator,
) {
    companion object {
        /** 生产环境基地址。 */
        const val BASE_URL = "https://app-v1.manxinghai.cn/api/"
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .authenticator(tokenAuthenticator)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}
