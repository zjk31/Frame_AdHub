package com.example.adhub

import android.app.Application
import org.koin.ksp.generated.defaultModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AdHubApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AdHubApp)
            // KSP 生成的模块（扫描 @Single, @KoinViewModel 等注解）
            modules(defaultModule)
        }
    }
}
