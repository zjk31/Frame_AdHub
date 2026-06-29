package com.example.adhub.core

import android.annotation.SuppressLint
import android.content.Context

/**
 * 全局 Application Context 持有者，供非 Activity 场景（如 AdProvider 初始化）获取 Context。
 */
@SuppressLint("StaticFieldLeak")
object AppContextHolder {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
