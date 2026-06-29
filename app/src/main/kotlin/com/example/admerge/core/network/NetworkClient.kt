package com.example.admerge.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 全局网络客户端工厂。
 */
object NetworkClient {

    /** 生产环境基地址（可通过 BuildConfig 覆盖）。 */
    const val BASE_URL = "https://app-v1.manxinghai.cn/api/"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            // TODO: AuthInterceptor (Token 注入 + 401 处理)
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
