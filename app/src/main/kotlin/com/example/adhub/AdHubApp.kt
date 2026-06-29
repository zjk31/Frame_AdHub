package com.example.adhub

import android.app.Application
import com.example.adhub.core.AppContextHolder
import com.example.adhub.data.AdSdkManager
import com.example.adhub.di.adModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class AdHubApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)

        startKoin {
            androidContext(this@AdHubApp)
            modules(adModule)
        }

        appScope.launch {
            val manager = GlobalContext.get().get<AdSdkManager>()
            manager.ensureProviderReady()
        }
    }
}
