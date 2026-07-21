package cn.manxinghai.zhuimange.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 全局 Gson 工厂 — 统一序列化配置。
 *
 * 双检锁单例，所有 Retrofit 实例共享同一份 Gson 配置，
 * 避免各模块重复创建 Gson 对象。
 */
object GsonFactory {

    @Volatile
    private var instance: Gson? = null

    /**
     * 创建/获取 Gson 实例。
     * 默认配置：宽松模式 + 跳过 null 字段。
     */
    fun create(): Gson {
        return instance ?: synchronized(this) {
            instance ?: GsonBuilder()
                .setLenient()
                .create() // 默认跳过 null 字段
                .also { instance = it }
        }
    }
}
